package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.api.AlipayConstants.CHARSET;
import static com.alipay.api.AlipayConstants.SIGN_TYPE;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;
    @Reference
    private PaymentService paymentService;
    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        request.setAttribute("orderId", orderId);
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        return "paymentIndex";
    }

    /**
     *   分析 ：
         1、通过orderId取得订单信息
         2、组合对应的支付信息保存到数据库。
         3、组合需要传给支付宝的参数。
         4、根据返回的表单生成html，传给浏览器。
     * @return
     */
    @RequestMapping("alipay/submit")
    @ResponseBody
    @LoginRequire
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        //Object attribute = request.getAttribute("");

        //1、通过orderId取得订单信息
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //保存支付信息 2、组合对应的支付信息保存到数据库。
        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setOrderId(orderId);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("过年买手机");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);

        paymentService.savePaymentInfo(paymentInfo);

        //3、组合需要传给支付宝的参数。

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 同步返回参数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步通知参数
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", paymentInfo.getTotalAmount());
        map.put("subject", paymentInfo.getSubject());

        alipayRequest.setBizContent(JSON.toJSONString(map));

        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    /**
     * 同步回调
     * @return
     */
    @RequestMapping("/alipay/callback/return")
    @LoginRequire
    public String callbackReturn(){
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 异步回调
     *   1、验证回调信息的真伪
         2、验证用户付款的成功与否
         3、把新的支付状态写入支付信息表{paymentInfo}中。
         4、通知电商
         5、给支付宝返回回执。
     *
     */
    @RequestMapping("/alipay/callback/notify")
    @LoginRequire
    public String paymentNotify(@RequestParam Map<String,String> paramsMap, HttpServletRequest request) throws AlipayApiException {

        /**
         * 1 保证支付的交易状态为TRADE_SUCCESS 或者 TRADE_FINISH
         *
         * 2 交易记录中的状态不能为PAID 和 CLOSED
         */

        //Map<String, String> paramsMap = //将异步通知中收到的所有参数都存放到map中

        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名

        if(signVerified){

            //验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
            // 校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

            //2 保证支付的交易状态为TRADE_SUCCESS 或者 TRADE_FINISH
            String trade_status = paramsMap.get("trade_status");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                //1 交易记录中的状态不能为PAID 和 CLOSED  获取payment_info中的payment_status值 不能为这两个 根据out_trade_no进行查找唯一对象
                String out_trade_no = paramsMap.get("out_trade_no");
                PaymentInfo paymentInfo1 = new PaymentInfo();
                paymentInfo1.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoHas = paymentService.getPaymentInfo(paymentInfo1);
                if (paymentInfoHas.getPaymentStatus()==PaymentStatus.PAID || paymentInfoHas.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                } else {
                    // 验签成功 更改状态 update paymentInfo set PaymentStatus=PaymentStatus.PAID where out_trade_no=?

                    PaymentInfo paymentInfoUpd = new PaymentInfo();
                    // 把支付状态改为已支付
                    paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                    // 添加回调时间 callback_time
                    paymentInfoUpd.setCallbackTime(new Date());
                    // 添加 内容体 （购买信息等等）
                    paymentInfoUpd.setCallbackContent(paramsMap.toString());
                    paymentService.updatePaymentInfo(out_trade_no, paymentInfoUpd);
                    return "success";
                }
            }

        }else{
            //验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    // sendPaymentResult
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo, @RequestParam("result")String result){
        paymentService.sendPaymentResult(paymentInfo, result);
        return "sent payment result";
    }

    // 主动询问支付结果
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
        Boolean flag = paymentService.checkPayment(paymentInfo);
        return ""+flag;
    }

}

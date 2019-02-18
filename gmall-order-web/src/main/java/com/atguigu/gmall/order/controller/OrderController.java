package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Reference
    private UserService userService;
    @Reference
    private CartService cartService;
    @Reference
    private OrderService orderService;

    /*@RequestMapping("/trade")
    @LoginRequire
    //@ResponseBody
    public List<UserAddress> findAddressByUserId(HttpServletRequest request){
        // 根据用户id得到用户地址
        String userId = (String) request.getAttribute("userId");

        List<UserAddress> addressList = userService.findUserAddressByUserId(userId);

        return null;
    }*/

    @RequestMapping("/trade")
    @LoginRequire
    //@ResponseBody
    public String findAddressByUserId(HttpServletRequest request){
        // 根据用户id得到用户地址
        String userId = (String) request.getAttribute("userId");

        //获取地址信息
        List<UserAddress> addressList = userService.findUserAddressByUserId(userId);
        request.setAttribute("addressList", addressList);

        //获取购物车中选中的信息集合
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);

        //订单详情信息
        List<OrderDetail> orderDetailList=new ArrayList<>(cartCheckedList.size());
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList", orderDetailList);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo", tradeNo);
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request){
        //初始化'

        String userId = (String) request.getAttribute("userId");
        String tradeCodeNo = request.getParameter("tradeNo");

        // 验证流水单号
        Boolean flag = orderService.checkTradeCode(userId,tradeCodeNo);
        if (flag){
            // 验证成功则删除流水单号
            orderService.delTradeCode(userId);
        } else {
            // 验证失败
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }

        orderInfo.setUserId(userId);

        // 验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result){
                // 库存不足
                request.setAttribute("errMsg","商品库存不足，请重新下单！");
                return "tradeFail";
            }
        }

        String orderId = orderService.saveOrder(orderInfo);
        return "redirect://payment.gmall.com/index?orderId="+orderId;
        //return orderId;
    }

    // 拆单控制器
    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        List<OrderInfo> subOrderInfoList = orderService.splitOrder(orderId,wareSkuMap);
        List<Map> wareMapList = new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            wareMapList.add(map);
        }
        return JSON.toJSONString(wareMapList);
    }

}

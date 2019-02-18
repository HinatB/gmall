package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {

    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据out_trade_no获取唯一payment对象
     * @param out_trade_no
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 修改支付信息
     * @param out_trade_no
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    /**
     * 发送消息，给activemq 支付结果
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 询问支付宝支付结果
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
        延迟队列反复调用
     * @param outTradeNo 单号
     * @param delaySec 延迟秒
     * @param checkCount 几次
     */
    void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount);

    void closePayment(String orderId);
}

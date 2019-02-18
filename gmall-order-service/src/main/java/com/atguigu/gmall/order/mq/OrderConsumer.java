package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){

        try {
            String orderId = mapMessage.getString("orderId");
            String result = mapMessage.getString("result");
            System.out.println("result = " + result);
            System.out.println("orderId = " + orderId);
            if ("success".equals(result)){
                //支付成功 修改订单状态为PAID
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                // 通知减库存
                orderService.sendOrderStatus(orderId);
                orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);

            } else {
                orderService.updateOrderStatus(orderId, ProcessStatus.UNPAID);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收库存发来的消息
     */
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage){

        try {
            String orderId = mapMessage.getString("orderId");
            String status = mapMessage.getString("status");
            if ("DEDUCTED".equals(status)){
                System.out.println("status = " + status);
                System.out.println("orderId = " + orderId);
                orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
            } else {
                orderService.updateOrderStatus(orderId, ProcessStatus.STOCK_EXCEPTION);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * try {
     String orderId = mapMessage.getString("orderId");
     String result = mapMessage.getString("result");
     System.out.println("result = " + result);
     System.out.println("orderId = " + orderId);
     if ("success".equals(result)){
     //支付成功 修改订单状态为PAID
     orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
     // 通知减库存
     orderService.sendOrderStatus(orderId);
     orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);

     } else {
     orderService.updateOrderStatus(orderId, ProcessStatus.UNPAID);
     }
     } catch (JMSException e) {
     e.printStackTrace();
     }
     */

}

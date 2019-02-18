package com.atguigu.gmall.order.test;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/*@EnableScheduling
@Component*/
public class OrderTask {

    @Autowired
    private OrderService orderService;


    /*@Scheduled(cron = "5 * * * * ?")
    public void work(){
        System.out.println("Thread ====== " + Thread.currentThread().getName());
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void work1(){
        System.out.println("Thread1 ====== " + Thread.currentThread().getName());
    }*/

    @Scheduled(cron = "0/20 * * * * ?")
    public  void checkOrder(){
        System.out.println("开始处理过期订单");
        Long starttime = System.currentTimeMillis();
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();
        for (OrderInfo orderInfo : expiredOrderList) {
            // 处理未完成订单
            orderService.execExpiredOrder(orderInfo);
        }
        long costtime = System.currentTimeMillis() - starttime;
        System.out.println("一共处理"+expiredOrderList.size()+"个订单 共消耗"+costtime+"毫秒");
    }

}

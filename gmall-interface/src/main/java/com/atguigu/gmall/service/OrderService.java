package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水单号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 验证流水单号
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    Boolean checkTradeCode(String userId, String tradeCodeNo);

    /**
     * 删除redis中的流水单号
     * @param userId
     */
    void delTradeCode(String userId);

    /**
     * 验证库存是否足够
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据orderId获取orderInfo对象
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 修改订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     *  通知减库存
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 收集过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理未完成订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     *
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}

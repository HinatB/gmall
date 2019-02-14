package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

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

}

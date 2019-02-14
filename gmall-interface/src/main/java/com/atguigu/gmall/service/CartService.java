package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 添加到购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 从redis或者从数据库中取得购物车集合
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListFromCookie
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);

    /**
     * 选中状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     *
     * 获取购物车中选中的项
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}

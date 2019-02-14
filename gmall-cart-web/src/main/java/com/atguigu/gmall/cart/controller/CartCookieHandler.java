package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

// 处理未登录时的购物车数据
@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String COOKIECARTNAME = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;

    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        /**
         * 1 先看购物车中是否有该商品
         * 2 有 则数量相加  update
         * 3 没有 则新添加到数据库  insert
         * 4 添加到cookie
         */

        String cookieValue = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);

        boolean ifExist = false;

        List<CartInfo> cartInfoList = new ArrayList<>();

        if (cookieValue!=null && cookieValue.length()>0) {
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);

            if (cartInfoList!=null && cartInfoList.size()>0){
                // 循环遍历 进行比较
                for (CartInfo cartInfo : cartInfoList) {

                    if (cartInfo.getSkuId().equals(skuId)) {
                        //说明存在该商品
                        cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                        cartInfo.setSkuPrice(cartInfo.getCartPrice());
                        ifExist = true;
                        break;
                    }
                }
            }
        }
        if (!ifExist){
            // 不存在
            //把商品信息取出来，新增到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo=new CartInfo();

            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfoList.add(cartInfo);
        }

        // 放入cookie中
        String newCartJson = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request, response, COOKIECARTNAME, newCartJson, COOKIE_CART_MAXAGE, true);

    }


    /**
     * 查询购物车的所有数据 cookie
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {
        List<CartInfo> cartInfos = null;
        String cookieValue = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        if (cookieValue!=null && cookieValue.length()>0) {
            //将字符串转换为集合
            cartInfos = JSON.parseArray(cookieValue, CartInfo.class);
        }
        return cartInfos;
    }

    /**
     * 删除cookie的没用数据
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, COOKIECARTNAME);
    }

    /**
     * 把cookie中的对应skuId的ischeck进行更改 并保存回去
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        List<CartInfo> cartList = getCartList(request);

        for (CartInfo cartInfo : cartList) {
            if (skuId.equals(cartInfo.getSkuId())){
                cartInfo.setIsChecked(isChecked);
            }
        }
        CookieUtil.setCookie(request, response, COOKIECARTNAME, JSON.toJSONString(cartList), COOKIE_CART_MAXAGE, true);
    }
}

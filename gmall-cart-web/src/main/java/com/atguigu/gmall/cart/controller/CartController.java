package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;
    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        String userId = (String) request.getAttribute("userId");
        if (userId!=null){
            // 登录了
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            //未登录 保存到cookie中
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = null;
        if (userId!=null){
            // 登录
            // 这里合并购物车
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);

            if (cartListCK!=null && cartListCK.size()>0){
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request,response);
            } else {
                //cookie中没有数据 查数据库
                cartInfoList = cartService.getCartList(userId);
            }

        } else {
            // 未登录
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList", cartInfoList);
        return "cartList";
    }

    /**
     * 勾选栏
     * @param request
     * @param response
     */
    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response){
        // var param="isChecked="+isCheckedFlag+"&"+"skuId="+skuId
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");

        if (userId!=null) {
            // 登录 更改数据库和redis中的数据
            cartService.checkCart(skuId,isChecked,userId);
        } else {
            //未登录 更改cookie中的数据
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }

    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);

        if (cartListCK!=null && cartListCK.size()>0){
            cartService.mergeToCartList(cartListCK, userId);
            cartCookieHandler.deleteCartCookie(request, response);
        }

        return "redirect://order.gmall.com/trade";
    }

}

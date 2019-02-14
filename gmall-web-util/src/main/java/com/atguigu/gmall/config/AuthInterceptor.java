package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.utils.HttpClientUtil;
import io.jsonwebtoken.impl.Base64Codec;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    //在进入控制器之前执行 判断是否存在token 是否登录
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //先得到token 只有在登陆完之后的跳转才有newtoken
        String token = request.getParameter("newToken");

        //登录的时候  token不为空 把token放在cookie中
        if (token!=null){
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
        }

        // request.getParameter("newToken");取不到值
        // 登陆后跳转其他业务时  没有newToken 所以取到的值为空 再在cookie中获取一次 赋值给token 判断是否已经登录
        if (token==null){
            token = CookieUtil.getCookieValue(request, "token", false);
        }
        //从cookie中取值后 若不为空 说明登陆过 为空 说明没登录
        if (token != null){

            Map map = getUserMapByToken(token);
            //获取用户名
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
        }

        // 判断自定义注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 获取注解
        LoginRequire loginRequireAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        //如果有注解 则做一个认证
        if (loginRequireAnnotation!=null){
            //获取salt
            String salt = request.getHeader("x-forwarded-for");
            // 远程调用verify
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS+ "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)){
                //认证成功
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId", userId);
                return true;
            } else {
                //认证失败
                //判断注解的autoRedirect是否为true
                if (loginRequireAnnotation.autoRedirect()){
                    // 为true 说明需要登录 重定向到登录页面 先获取是从哪个页面过来的
                    String requestURL = request.getRequestURL().toString();
                    //对url进行编码
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }
        }



        return true;
    }

    private Map getUserMapByToken(String token) {
        //token中间部分是用户信息  取到中间部分
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        //解密
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        //转换成字符串类型
        String tokenJson = null;

        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 转换为map
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;

    }


}

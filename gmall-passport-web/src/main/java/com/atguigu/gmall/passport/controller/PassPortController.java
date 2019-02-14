package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassPortController {

    @Reference
    private UserService userService;
    @Value("token.key")
    private String tokenKey;

    @RequestMapping("/index")
    public String index(Model model, HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        //存储originUrl
        model.addAttribute("originUrl", originUrl);

        return "index";
    }

    @RequestMapping("/login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request){

        // 得到用户名 密码
        UserInfo info = userService.login(userInfo);
        if (info!=null){
            //生成token

            String key = tokenKey;
            Map<String, Object> map = new HashMap<>();
            map.put("userId", info.getId());
            System.out.println("userId:  "+ info.getId());
            map.put("nickName", info.getNickName());
            // ip 要从服务器中获取
            String salt = request.getHeader("X-forwarded-for");
            //System.out.println("remoteAddr: " + salt);
            String token = JwtUtil.encode(key, map, salt);
            System.out.println("token :" + token);
            return token;
        } else {
            return "fail";
        }
    }

    //验证
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");

        //得到用户信息
        Map<String, Object> map = JwtUtil.decode(token, tokenKey, salt);

        //判断在redis中是否存在
        UserInfo userInfo = userService.verify(map.get("userId"));
        if (userInfo!=null){
            return "success";
        }
        return "fail";
    }

}

package com.atguigu.gmall.passport.config;

import io.jsonwebtoken.*;

import java.util.Map;

public class JwtUtil {
    //加密 生成token String key 公共部分,Map<String,Object> param 私有部分,String salt
    public static String encode(String key,Map<String,Object> param,String salt){
        if(salt!=null){
            key+=salt;
        }
        //用加密算法进行加密
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256,key);

        //把用户信息加进去
        jwtBuilder = jwtBuilder.setClaims(param);

        //生成token
        String token = jwtBuilder.compact();
        return token;

    }


    //解密 通过key和salt 解出来是私有部分 ：用户信息
    public  static Map<String,Object> decode(String token , String key, String salt){
        Claims claims=null;
        if (salt!=null){
            key+=salt;
        }
        try {
            claims= Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch ( JwtException e) {
            return null;
        }
        return  claims;
    }

}

package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 使当前类RedisConfig变成配置文件 xxx.xml
@Configuration
public class RedisConfig {

    // 给host,port,database 赋值
    // 使用@Value 注解前提条件是当前类必须被spring 容器管理！
    // :disabled 表示如果没有取得到host，则给一个默认值disabled
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;


    // 将host,port,database  三个参数给RedisUtil 中的初始化方法
    // 将RedisUtil 注入到容器中
    @Bean
    public RedisUtil getRedisUtil(){
        if ("disabled".equals(host)){
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,database);
        return redisUtil;
    }


}

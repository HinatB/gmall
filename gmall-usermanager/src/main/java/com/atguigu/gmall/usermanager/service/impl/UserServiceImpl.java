package com.atguigu.gmall.usermanager.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.usermanager.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanager.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String USERKEY_PREFIX="user:";
    public String USERINFOKEY_SUFFIX=":info";
    public int USERKEY_TIMEOUT=60*60*24;

    @Override
    public List<UserInfo> finaAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> findUserAddressByUserId(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        // 先把密码改为密文
        String passwd = userInfo.getPasswd();
        String newpasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newpasswd);
        // 从数据库中查找
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info!=null){
            //存到redis中
            Jedis jedis = redisUtil.getJedis();
            String userKey = USERKEY_PREFIX + info.getId() + USERINFOKEY_SUFFIX;
            jedis.setex(userKey, USERKEY_TIMEOUT, JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(Object userId) {
        Jedis jedis = redisUtil.getJedis();
        String userIdForSearch = USERKEY_PREFIX + userId + USERINFOKEY_SUFFIX;
        String userJson = jedis.get(userIdForSearch);
        if (userJson!=null){
            UserInfo info = JSON.parseObject(userJson, UserInfo.class);
            jedis.expire(userIdForSearch, USERKEY_TIMEOUT);
            jedis.close();
            return info;
        }
        jedis.close();
        return null;
    }
}

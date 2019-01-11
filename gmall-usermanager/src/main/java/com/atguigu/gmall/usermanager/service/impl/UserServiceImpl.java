package com.atguigu.gmall.usermanager.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.usermanager.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanager.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;

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
}

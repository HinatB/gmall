package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    /**
     * 查询所有用户
     * @return
     */
    List<UserInfo> finaAll();

    /**
     * 根据用户id查询地址
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressByUserId(String userId);

}

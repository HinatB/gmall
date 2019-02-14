package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CartInfoMapper cartInfoMapper;

    /**
     * 用户登录时
     * 加入到购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /**
         * 1 先看购物车中是否有该商品
         * 2 有 则数量相加  update
         * 3 没有 则新添加到数据库  insert
         * 4 方便查询 添加到redis中
         */
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

        if (cartInfoExist!=null) {
            // 存在
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        } else {
            // 不存在
            CartInfo cartInfo1 = new CartInfo();
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfoMapper.insertSelective(cartInfo1);

            cartInfoExist = cartInfo1;
        }

        /**
         * 放入redis中
         * 采用hash的数据结构
         * 1构建key user:userid:cart
         * 2 jedis.hset(key, field, value) 用skuId当field value就是cartInfoExist
         */
        Jedis jedis = redisUtil.getJedis();
        String key = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = JSON.toJSONString(cartInfoExist);

        jedis.hset(key, skuId, cartJson);


    }

    @Override
    public List<CartInfo> getCartList(String userId) {

        List<CartInfo> cartInfos = new ArrayList<>();

        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 制作key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 根据key 获取redis中的所有数据
        //jedis.hgetAll(userCartKey);
        List<String> cartJsons = jedis.hvals(userCartKey);
        if (cartJsons!= null && cartJsons.size() >0) {
            // 遍历 转换为 List<CartInfo>
            for (String cartJson : cartJsons) {
                cartInfos.add(JSON.parseObject(cartJson, CartInfo.class));
            }
            // 排序
            cartInfos.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
        } else {
            //从数据库中查
            cartInfos = loadCartCache(userId);
        }

        jedis.close();
        return cartInfos;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();

        List<CartInfo> cartListDB = cartInfoMapper.selectCartListWithCurPrice(userId);

        for (CartInfo cartInfoCK : cartListCK) {
            boolean isMatch =false;

            if (cartListDB!=null && cartListDB.size()>0) {

                for (CartInfo cartInfoDB : cartListDB) {
                    if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())) {
                        // cookie和数据库中都有 数量相加
                        cartInfoDB.setSkuNum(cartInfoCK.getSkuNum() + cartInfoDB.getSkuNum());
                        // 更新
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                        isMatch = true;
                    }
                }
            }
            if (!isMatch) {
                // 没有相同的 直接插入数据库
                // cartInfoCK中没有userId 补充userId
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        //合并完后再查
        cartInfos = loadCartCache(userId);
        // 合并选中的商品
        for (CartInfo cartInfoDB : cartInfos) {
            for (CartInfo cartInfoCK : cartListCK) {
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    //以cookie的为基准
                    if ("1".equals(cartInfoCK.getIsChecked())){
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());
                        checkCart(cartInfoCK.getSkuId(), cartInfoCK.getIsChecked(), userId);
                    }
                }
            }
        }

        return cartInfos;
    }

    /**
     * 购物车中选中的勾选栏
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /**
         *   把对应skuId的购物车的信息从redis中取出来，反序列化，修改isChecked标志。
             再保存回redis中。
             同时保存另一个redis的key 专门用来存储用户选中的商品，方便结算页面使用。
         */
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        // 修改ischeck状态
        cartInfo.setIsChecked(isChecked);
        // 保存回redis中
        String cartCheckdJson = JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey, skuId, cartCheckdJson);
        // 同时保存另一个redis的key 专门用来存储用户选中的商品，方便结算页面使用。
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        if ("1".equals(isChecked)){
            //选中状态 添加
            jedis.hset(userCheckedKey, skuId, cartCheckdJson);
        } else {
            //未选中状态 删除
            jedis.hdel(userCheckedKey, skuId);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);

        List<CartInfo> newList = new ArrayList<>();
        for (String cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            newList.add(cartInfo);
        }

        jedis.close();
        return newList;
    }

    /**
     * 根据userId去数据库查找购物车数据 并保存到redis中
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {

        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoList==null || cartInfoList.size()==0) {
            return null;
        }

        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Map<String, String> map = new HashMap<>(cartInfoList.size());

        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
        }

        jedis.hmset(userCartKey, map);
        jedis.close();
        return cartInfoList;

    }
}

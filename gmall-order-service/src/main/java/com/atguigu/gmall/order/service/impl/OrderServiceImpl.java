package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String USERORDERKEY_PREFIX="user:";
    public String USERORDERKEY_SUFFIX=":tradeCode";
    public int USERORDERKEY_TIMEOUT=60*10;

    @Override
    public String saveOrder(OrderInfo orderInfo) {
        //缺少
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间 利用日期函数
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        //进程状态 订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        // 第三方交易编号 随机生成的
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        orderInfo.sumTotalAmount();

        orderInfoMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        // 插入订单详细信息
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        String orderId = orderInfo.getId();
        return orderId;
    }

    /**
     * 把生成的流水单号放入redis中
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        String tradeNoKey = USERORDERKEY_PREFIX + userId + USERORDERKEY_SUFFIX;
        String tradeCode = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(tradeNoKey, USERORDERKEY_TIMEOUT, tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public Boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = USERORDERKEY_PREFIX + userId + USERORDERKEY_SUFFIX;
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        if (tradeCode!=null && tradeCode.equals(tradeCodeNo)){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void delTradeCode(String userId) {
        String tradeNoKey = USERORDERKEY_PREFIX + userId + USERORDERKEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return true;
        } else {
            return false;
        }
    }
}

package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.utils.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Reference
    private PaymentService paymentService;

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

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo1 = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo1.setOrderDetailList(orderDetailList);
        return orderInfo1;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
            //Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);
            producer.send(textMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime",new Date()).andEqualTo("processStatus",ProcessStatus.UNPAID);
        return orderInfoMapper.selectByExample(example);
    }

    /**
     * 处理未完成订单
     * @param orderInfo
     */
    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 修改订单表的状态信息
        updateOrderStatus(orderInfo.getId(), ProcessStatus.CLOSED);
        // 付款信息 修改paymentInfo 的 Payment_status状态
        paymentService.closePayment(orderInfo.getId());
    }

    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map<String, Object> details = initWareOrder(orderInfo);
        return JSON.toJSONString(details);
    }

    public Map initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", "测试减库存通知");
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId",orderInfo.getWareId());
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<Map> details = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("skuId", orderDetail.getSkuId());
            map1.put("skuNum", orderDetail.getSkuNum());
            map1.put("skuName", orderDetail.getSkuName());
            details.add(map1);
        }
        map.put("details", details);
        return map;
    }

    /**
     * 拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 1 先查询原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // 2 wareSkuMap 反序列化 (仓库编号与商品的对照关系) [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        // 3 遍历拆单方案
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 4 生成订单主表，从原始订单复制，新的订单号，父订单
            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(subOrderInfo, orderInfoOrigin);
            subOrderInfo.setId(null);
            // 5 原来主订单，订单主表中的订单状态标志为拆单
            subOrderInfo.setParentOrderId(orderId);
            subOrderInfo.setWareId(wareId);
            // 6 明细表 根据拆单方案中的skuids进行匹配，得到那个的子订单
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            // 创建一个新的订单详情集合
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    if (skuId.equals(orderDetail.getSkuId())){
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            subOrderInfo.sumTotalAmount();
            // 7 保存到数据库中
            saveOrder(subOrderInfo);
            subOrderInfoList.add(subOrderInfo);
        }
        updateOrderStatus(orderId, ProcessStatus.SPLIT);
        // 8 返回一个新生成的子订单列表
        return subOrderInfoList;
    }
}

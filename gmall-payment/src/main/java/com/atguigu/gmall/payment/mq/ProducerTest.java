package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {
    public static void main(String[] args) {
        // 1 创建工厂
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.32.129:61616");
        // 2 创建链接
        try {
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // 创建session 第一个参数表示是否支持事务，false时，
            // 第二个参数
            // Session.AUTO_ACKNOWLEDGE 自动，
            // SESSION_TRANSACTED 开启事务
            // Session.CLIENT_ACKNOWLEDGE手动签收事务，
            // DUPS_OK_ACKNOWLEDGE 批量订阅
            // 其中一个
            // 第一个参数设置为true时，第二个参数可以忽略 服务器设置为SESSION_TRANSACTED
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // 创建队列
            Queue queue = session.createQueue("Hina");
            MessageProducer producer = session.createProducer(queue);
            //创建消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText("hello");

            producer.send(activeMQTextMessage);
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}

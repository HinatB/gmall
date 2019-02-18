package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ConsumerTest {
    public static void main(String[] args) {
        // 1 创建工厂
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, ActiveMQConnection.DEFAULT_PASSWORD, "tcp://192.168.32.129:61616");
        // 2 创建连接
        try {
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // 3 创建会话
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // 4 创建队列
            Queue queue = session.createQueue("Hina");
            // 5 创建consumer
            MessageConsumer consumer = session.createConsumer(queue);
            // 6 接收消息
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    // 参数就是接收的消息
                    if (message instanceof TextMessage){
                        try {
                            String text = ((TextMessage) message).getText();
                            System.out.println("接收的消息: " + text);
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}

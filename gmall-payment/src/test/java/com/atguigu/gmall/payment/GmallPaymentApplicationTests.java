package com.atguigu.gmall.payment;

import com.atguigu.gmall.config.ActiveMQUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

	@Autowired
	private ActiveMQUtil activeMQUtil;

	@Test
	public void testActiveMq() throws JMSException {
		Connection connection = activeMQUtil.getConnection();
		connection.start();

		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		// 创建队列
		Queue queue = session.createQueue("TESTMQUTIL");
		MessageProducer producer = session.createProducer(queue);
		//创建消息对象
		ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
		activeMQTextMessage.setText("测试工具类");

		producer.send(activeMQTextMessage);
		producer.close();
		session.close();
		connection.close();
	}

	@Test
	public void contextLoads() {
	}

}


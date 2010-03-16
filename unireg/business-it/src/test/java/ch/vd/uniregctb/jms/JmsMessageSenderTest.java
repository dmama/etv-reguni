package ch.vd.uniregctb.jms;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.jencks.amqpool.JcaPooledConnectionFactory;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * Ce test n'est pas un vrai test: il permet juste de simuler l'envoi d'un message pour du debug manuel.
 */
@RunWith(JUnit4ClassRunner.class)
public class JmsMessageSenderTest {

	private static final Logger LOGGER = Logger.getLogger(JmsMessageSenderTest.class);

	@Test
	public void testSendMessage() throws Exception {

		//sendMessage();
	}


	public void sendMessage() throws Exception {

		try {

			//BufferedReader reader = new BufferedReader(new FileReader("sdiDocTest01.xml"));

			InputStream stream = getClass().getResourceAsStream("/jms/EvtRegCivil-jms-message.xml");
			InputStreamReader reader = new InputStreamReader(stream);

			final StringBuffer sb = new StringBuffer(1024);
			char[] chars = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(chars)) > -1) {
				sb.append(String.valueOf(chars, 0, numRead));
			}
			reader.close();

			ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
			jmsConnectionManager.setBrokerURL("tcp://grominet:4500");
			jmsConnectionManager.setUserName("smx");
			jmsConnectionManager.setPassword("smx");

			JcaPooledConnectionFactory jmsConnectionFactory = new JcaPooledConnectionFactory();
			jmsConnectionFactory.setConnectionFactory(jmsConnectionManager);
			jmsConnectionFactory.setMaxConnections(8);
			jmsConnectionFactory.setName("Unireg-ActiveMQ");
			//jmsConnectionFactory.setTransactionManager(transactionManager);

			JmsTemplate jmsTemplate = new JmsTemplate(jmsConnectionFactory);

			jmsTemplate.send("ch.vd.registre.evtCivil", new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage tm = session.createTextMessage();
					tm.setText(sb.toString());
					return tm;
				}
			});

		}
		finally {
		}

		LOGGER.info("Terminé");
	}

}

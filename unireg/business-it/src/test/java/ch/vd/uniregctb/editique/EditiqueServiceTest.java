package ch.vd.uniregctb.editique;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactoryBean;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.uniregctb.common.BusinessItTest;

public class EditiqueServiceTest extends BusinessItTest {

	//private static final Logger LOGGER = Logger.getLogger(EditiqueServiceTest.class);

	private EditiqueService editiqueService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		editiqueService = getBean(EditiqueService.class, "editiqueService");
	}

	/**
	 * Teste la methode de recuperation d'un document
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDocument() throws Exception {
		sendMessage();
		editiqueService.getDocument("InputPdf", false);
	}

	/**
	 * Teste la methode d'envoi de document immediate
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerDocumentImmediatement() throws Exception {
		editiqueService.creerDocumentImmediatement("nomDocument", "typeDocument", TypeFormat.PDF, new Object(), false);
	}

	/**
	 * Teste la methode d'envoi de document par batch
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerDocumentParBatch() throws Exception {
		editiqueService.creerDocumentParBatch(new Object(), null, false);
	}

	/**
	 * Simulation d'envoie de message dans la queue de reception en provenance de l'editique
	 * ch.vd.uniregctb.EditiqueInputJMSQueue
	 *
	 * @throws Exception
	 */
	public void sendMessage() throws Exception {

		try {

			InputStream stream = getClass().getResourceAsStream("EditiqueInputPdf.xml");
			InputStreamReader reader = new InputStreamReader(stream);

			final StringBuffer sb = new StringBuffer(1024);
			char[] chars = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(chars)) > -1) {
				sb.append(String.valueOf(chars, 0, numRead));
			}
			reader.close();

			ActiveMQConnectionFactory jmsConnectionManager = new ActiveMQConnectionFactory();
			jmsConnectionManager.setBrokerURL("vm://localhost");

			PooledConnectionFactoryBean jmsConnectionFactory = new PooledConnectionFactoryBean();
			jmsConnectionFactory.setConnectionFactory(jmsConnectionManager);
			jmsConnectionFactory.setMaxConnections(8);
			//jmsConnectionFactory.setTransactionManager(transactionManager);

			JmsTemplate jmsTemplate = new JmsTemplate(jmsConnectionFactory.getConnectionFactory());

			jmsTemplate.send("ch.vd.editique.output", new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage tm = session.createTextMessage();
					tm.setText(sb.toString());
					return tm;
				}
			});
		}
		finally {
		}
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}
}

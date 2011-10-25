package ch.vd.uniregctb.evenement;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import java.util.Date;
import java.util.Map;

import org.apache.activemq.ra.ActiveMQResourceAdapter;
import org.jetbrains.annotations.Nullable;
import org.junit.After;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.jms.EsbMessageEndpointManager;
import ch.vd.uniregctb.utils.UniregProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class EvenementTest {

	protected EsbJmsTemplate esbTemplate;

	protected EsbMessageFactory esbMessageFactory;
	protected UniregProperties uniregProperties;
	protected ConnectionFactory jmsConnectionFactory;
	protected ActiveMQResourceAdapter resourceAdapter;
	protected EsbMessageEndpointManager manager;

	protected EvenementTest() {
		EvenementHelper.initLog4j();
		uniregProperties = EvenementHelper.initProps();
		jmsConnectionFactory = EvenementHelper.initConnectionFactory(uniregProperties);
		resourceAdapter = EvenementHelper.initResourceAdapter(uniregProperties);
	}

	@After
	public void tearDown() {
		if (manager != null) {
			manager.destroy();
			manager = null;
		}
	}

	protected void initEndpointManager(String queueName, MessageListener listener) {
		manager = EvenementHelper.initEndpointManager(resourceAdapter, queueName, listener);
	}

	protected void clearQueue(String queueName) throws Exception {
		EvenementHelper.clearQueue(esbTemplate, queueName);
	}

	protected void assertTextMessage(String queueName, final String texte) throws Exception {

		esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
		final EsbMessage msg = esbTemplate.receive(queueName);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		String actual = msg.getBodyAsString();
		actual = actual.replaceAll(" standalone=\"(no|yes)\"", ""); // on ignore l'attribut standalone s'il existe
		assertEquals(texte, actual);

		final EsbMessage noMsg = esbTemplate.receive(queueName);
		assertNull(noMsg);
	}

	protected void sendTextMessage(String queueName, String texte, @Nullable Map<String, String> customAttributes) throws Exception {
		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		if (customAttributes != null) {
			for (Map.Entry<String, String> attr : customAttributes.entrySet()) {
				m.addHeader(attr.getKey(), attr.getValue());
			}
		}
		esbTemplate.send(m);
	}

	protected void sendTextMessage(String queueName, String texte) throws Exception {
		sendTextMessage(queueName, texte, null);
	}

	/**
	 * @param year  l'année
	 * @param month le mois (1-12)
	 * @param day   le jour (1-31)
	 * @return une date initialisée au jour, mois et année spécifiés.
	 */
	protected static Date newUtilDate(int year, int month, int day) {
		return EvenementHelper.newUtilDate(year, month, day);
	}
}


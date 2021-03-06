package ch.vd.unireg.evenement;

import javax.jms.ConnectionFactory;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.jms.connection.JmsTransactionManager;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.jms.EsbBusinessErrorCollector;
import ch.vd.unireg.jms.EsbBusinessErrorHandler;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.jms.GentilEsbMessageListenerContainer;
import ch.vd.unireg.utils.UniregProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class EvenementTest {

	protected EsbJmsTemplate esbTemplate;
	protected EsbMessageValidator esbValidator;

	protected UniregProperties uniregProperties;
	protected ConnectionFactory jmsConnectionFactory;
	protected GentilEsbMessageListenerContainer listener;

	protected EvenementTest() {
		uniregProperties = EvenementHelper.initProps();
		jmsConnectionFactory = EvenementHelper.initConnectionFactory(uniregProperties);
	}

	@Before
	public void setUp() throws Exception {
		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}
	}

	@After
	public void tearDown() {
		esbValidator = null;
		if (listener != null) {
			listener.stop();
			listener.destroy();
		}
	}

	protected void buildEsbMessageValidator(Resource[] sources) throws Exception {
		esbValidator = BusinessItTest.buildEsbMessageValidator(sources);
	}

	protected void buildEsbMessageValidator(String[] locationPatterns) throws Exception {
		esbValidator = BusinessItTest.buildEsbMessageValidator(locationPatterns);
	}

	protected void initListenerContainer(String queueName, EsbMessageHandler handler, EsbBusinessErrorHandler errorHandler) {
		listener = new GentilEsbMessageListenerContainer();
		listener.setHandler(handler);
		listener.setEsbErrorHandler(errorHandler);
		listener.setEsbTemplate(esbTemplate);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setConnectionFactory(jmsConnectionFactory);
		listener.setDestinationName(queueName);
		listener.afterPropertiesSet();
		listener.start();
	}

	protected void initListenerContainer(String queueName, EsbMessageHandler handler) {
		initListenerContainer(queueName, handler, new EsbBusinessErrorCollector());
	}

	protected void clearQueue(String queueName) throws Exception {
		EvenementHelper.clearQueue(esbTemplate, queueName);
	}

	private String receiveTextMessage(String queueName) throws Exception {
		esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
		final EsbMessage msg = esbTemplate.receive(queueName);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		String actual = msg.getBodyAsString();
		actual = actual.replaceAll(" standalone=\"(no|yes)\"", ""); // on ignore l'attribut standalone s'il existe
		return actual;
	}

	private void checkNoMoreMessage(String queueName) throws Exception {
		final EsbMessage noMsg = esbTemplate.receive(queueName);
		assertNull(noMsg);
	}

	protected void assertTextMessage(String queueName, final String texte) throws Exception {
		final String actual = receiveTextMessage(queueName);
		assertEquals(texte, actual);
		checkNoMoreMessage(queueName);
	}

	protected void assertTextMessage(String queueName, Pattern pattern) throws Exception {
		final String actual = receiveTextMessage(queueName);
		final Matcher matcher = pattern.matcher(actual);
		assertTrue(matcher.matches());
		checkNoMoreMessage(queueName);
	}

	protected void sendTextMessage(String queueName, String texte, @Nullable Map<String, String> customAttributes) throws Exception {
		sendTextMessage(queueName, texte, null, customAttributes);
	}


	protected void sendTextMessage(String queueName, String texte, String businessId, @Nullable Map<String, String> customAttributes) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		final String myBusinessId = businessId == null ? String.valueOf(m.hashCode()) : businessId;
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(myBusinessId);
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		if (customAttributes != null) {
			EsbMessageHelper.setHeaders(m, customAttributes, true);
		}
		esbValidator.validate(m);
		esbTemplate.send(m);
	}

	protected void sendTextMessage(String queueName, String texte) throws Exception {
		sendTextMessage(queueName, texte, (Map<String, String>) null);
	}

	protected void sendTextMessage(String queueName, String texte, String businessId) throws Exception {
		sendTextMessage(queueName, texte, businessId, null);
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


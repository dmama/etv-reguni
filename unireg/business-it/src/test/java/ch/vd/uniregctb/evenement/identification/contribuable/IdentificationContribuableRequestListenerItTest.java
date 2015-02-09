package ch.vd.uniregctb.evenement.identification.contribuable;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementHelper;
import ch.vd.uniregctb.jms.EsbMessageValidator;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml",
		"classpath:ut/unireg-businessit-jms-evt-ident.xml"
})
public abstract class IdentificationContribuableRequestListenerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String inputQueue;
	private String OutputQueue;
	private EsbMessageValidator esbValidator;
	private SmartLifecycle endpointManager;

	public IdentificationContribuableRequestListenerItTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		esbValidator = buildEsbMessageValidator(new Resource[]{new ClassPathResource(getRequestXSD()), new ClassPathResource(getResponseXSD())});

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.ident.ctb.input");
		OutputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue);
		EvenementHelper.clearQueue(esbTemplate, OutputQueue);

		endpointManager = getBean(SmartLifecycle.class, "jmsIdentCtbEndpointManagerAutomatique");
		endpointManager.start();
	}

	@Override
	public void onTearDown() throws Exception {
		endpointManager.stop();
		super.onTearDown();
	}

	protected abstract String getRequestXSD();
	protected abstract String getResponseXSD();

	protected EsbJmsTemplate getEsbTemplate() {
		return esbTemplate;
	}

	protected String getInputQueue() {
		return inputQueue;
	}

	protected String getOutputQueue() {
		return OutputQueue;
	}

	protected EsbMessage buildTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.setServiceReplyTo(replyTo);
		return m;
	}

	protected void sendTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
		esbValidator.validate(m);
		getEsbTemplate().send(m);
	}

	protected EsbMessage getEsbMessage(String queue) throws Exception {
		getEsbTemplate().setReceiveTimeout(10000);        // On attend le message jusqu'à 10 secondes
		final EsbMessage msg = getEsbTemplate().receive(queue);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}
}

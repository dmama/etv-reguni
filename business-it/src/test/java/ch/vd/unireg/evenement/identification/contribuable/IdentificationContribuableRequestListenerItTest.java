package ch.vd.unireg.evenement.identification.contribuable;

import java.util.LinkedHashSet;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.SmartLifecycle;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.jms.EsbMessageValidator;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public abstract class IdentificationContribuableRequestListenerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String inputQueue;
	private String outputQueue;
	private EsbMessageValidator esbValidator;
	private SmartLifecycle endpointManager;
	protected LinkedHashSet<String> xsdPathes;

	public IdentificationContribuableRequestListenerItTest() {
		setWantIndexationTiers(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		final IdentificationContribuableRequestHandler handler = getBean(IdentificationContribuableRequestHandler.class, getHandlerName());

		xsdPathes = new LinkedHashSet<>();
		xsdPathes.addAll(handler.getRequestXSDs());
		xsdPathes.addAll(handler.getResponseXSDs());
		esbValidator = buildEsbMessageValidator(XmlUtils.toResourcesArray(xsdPathes));

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.ident.ctb.input");
		outputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue, transactionManager);
		EvenementHelper.clearQueue(esbTemplate, outputQueue, transactionManager);

		endpointManager = getBean(SmartLifecycle.class, "identCtbMessageListenerAutomatique");
		endpointManager.start();
	}

	@Override
	public void onTearDown() throws Exception {
		endpointManager.stop();
		super.onTearDown();
	}

	@NotNull
	protected abstract String getHandlerName();

	protected EsbJmsTemplate getEsbTemplate() {
		return esbTemplate;
	}

	protected String getInputQueue() {
		return inputQueue;
	}

	protected String getOutputQueue() {
		return outputQueue;
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
		EvenementHelper.sendMessage(esbTemplate, m, transactionManager);
	}

	protected EsbMessage getEsbMessage(String queue) throws Exception {
		final EsbMessage msg = EvenementHelper.getMessage(esbTemplate, queue, 10000, transactionManager);       // On attend le message jusqu'à 10 secondes
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}
}

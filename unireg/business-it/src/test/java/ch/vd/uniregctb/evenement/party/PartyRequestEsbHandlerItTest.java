package ch.vd.uniregctb.evenement.party;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementHelper;
import ch.vd.uniregctb.jms.EsbBusinessErrorCollector;
import ch.vd.uniregctb.jms.EsbMessageValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Factorise le code commun pour les autres classes concrètes du package
 */
@SuppressWarnings({"JavaDoc"})
public abstract class PartyRequestEsbHandlerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String inputQueue;
	private String outputQueue;
	private EsbMessageValidator esbValidator;
	private EsbBusinessErrorCollector errorCollector;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		errorCollector = getBean(EsbBusinessErrorCollector.class, "partyRequestErrorCollector");
		errorCollector.clear();

		final List<Resource> sources = new ArrayList<>();
		sources.add(new ClassPathResource(getRequestXSD()));
		for (String xsd : getResponseXSD()) {
			sources.add(new ClassPathResource(xsd));
		}
		esbValidator = buildEsbMessageValidator(sources.toArray(new Resource[sources.size()]));

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.party.service");
		outputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue, transactionManager);
		EvenementHelper.clearQueue(esbTemplate, outputQueue, transactionManager);
	}

	protected abstract List<String> getResponseXSD();

	protected abstract String getRequestXSD();

	protected void deactivateEsbValidator() {
		esbValidator = null;
	}

	protected EsbJmsTemplate getEsbTemplate() {
		return esbTemplate;
	}

	protected String getInputQueue() {
		return inputQueue;
	}

	protected String getOutputQueue() {
		return outputQueue;
	}

	protected EsbBusinessErrorCollector getErrorCollector() {
		return errorCollector;
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

	/**
	 * @return le businessId du message envoyé
	 */
	protected String sendTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
		validateMessage(m);
		EvenementHelper.sendMessage(esbTemplate, m, transactionManager);
		return m.getBusinessId();
	}

	protected void validateMessage(EsbMessage msg) throws Exception {
		if (esbValidator != null) {
			esbValidator.validate(msg);
		}
	}

	protected EsbMessage getEsbMessage(String queue) throws Exception {
		final EsbMessage msg = EvenementHelper.getMessage(esbTemplate, queue, 10000, transactionManager);       // On attend le message jusqu'à 10 secondes
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}

	protected EsbMessage getEsbBusinessErrorMessage() throws Exception {
		final List<EsbMessage> collected = errorCollector.waitForIncomingMessages(1, 10000);       // on attend le message jusqu'à 10 secondes
		assertNotNull("L'événement n'a pas été posé en queue d'erreur", collected);
		assertEquals(1, collected.size());
		return collected.get(0);
	}
}

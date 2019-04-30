package ch.vd.unireg.evenement.party;

import java.util.LinkedHashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.evenement.infra.RequestHandler;
import ch.vd.unireg.jms.EsbBusinessErrorCollector;
import ch.vd.unireg.jms.EsbMessageValidator;

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
	protected LinkedHashSet<String> xsdPathes;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		errorCollector = getBean(EsbBusinessErrorCollector.class, "partyRequestErrorCollector");
		errorCollector.clear();

		xsdPathes = new LinkedHashSet<>();
		final Object handler = getBean(Object.class, getRequestHandlerName());
		if (handler instanceof RequestHandler) {
			final RequestHandler<?> handlerV1 = (RequestHandler<?>) handler;
			xsdPathes.addAll(handlerV1.getRequestXSDs());
			xsdPathes.addAll(handlerV1.getResponseXSDs());
		}
		else if (handler instanceof RequestHandlerV1) {
			final RequestHandlerV1<?> handlerV1 = (RequestHandlerV1<?>) handler;
			xsdPathes.addAll(handlerV1.getRequestXSDs());
			xsdPathes.addAll(handlerV1.getResponseXSDs());
		}
		else if (handler instanceof RequestHandlerV2) {
			final RequestHandlerV2<?> handlerV2 = (RequestHandlerV2<?>) handler;
			xsdPathes.addAll(handlerV2.getRequestXSDs());
			xsdPathes.addAll(handlerV2.getResponseXSDs());
		}
		else {
			throw new ProgrammingException("Type de handler inconnu = [" + handler.getClass() + "]");
		}

		esbValidator = buildEsbMessageValidator(XmlUtils.toResourcesArray(xsdPathes));

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.party.service");
		outputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue, transactionManager);
		EvenementHelper.clearQueue(esbTemplate, outputQueue, transactionManager);
	}

	/**
	 * @return le nom de bean Spring du handler qui est testé.
	 */
	@NotNull
	protected abstract String getRequestHandlerName();

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

	protected EsbMessage buildTextMessage(String queueName, String texte, String replyTo) {
		try {
			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessUser("EvenementTest");
			m.setBusinessId(String.valueOf(m.hashCode()));
			m.setContext("test");
			m.setServiceDestination(queueName);
			m.setBody(texte);
			m.setServiceReplyTo(replyTo);
			return m;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return le businessId du message envoyé
	 */
	protected String sendTextMessage(String queueName, String texte, String replyTo) {
		final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
		validateMessage(m);
		EvenementHelper.sendMessage(esbTemplate, m, transactionManager);
		return m.getBusinessId();
	}

	protected void validateMessage(EsbMessage msg) {
		if (esbValidator != null) {
			try {
				esbValidator.validate(msg);
			}
			catch (ESBValidationException e) {
				throw new RuntimeException(e);
			}
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

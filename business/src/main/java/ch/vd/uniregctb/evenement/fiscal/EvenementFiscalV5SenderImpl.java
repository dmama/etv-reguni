package ch.vd.uniregctb.evenement.fiscal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.fiscal.v5.FiscalEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ObjectFactory;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.utils.LogLevel;

/**
 * Bean qui permet d'envoyer des événements externes (en version 5).
 */
public class EvenementFiscalV5SenderImpl implements EvenementFiscalSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementFiscalV5SenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private EvenementFiscalV5Factory evenementFiscalV5Factory;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private JAXBContext jaxbContext;

	/**
	 * for testing purposes
	 */
	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setEvenementFiscalV5Factory(EvenementFiscalV5Factory evenementFiscalV5Factory) {
		this.evenementFiscalV5Factory = evenementFiscalV5Factory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}

		final FiscalEvent event = evenementFiscalV5Factory.buildOutputData(evenement);
		if (event == null) {
			// mapping inexistant pour le canal v5 -> on abandonne
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Evenement fiscal %d (%s) sans équivalent dans le canal v5 -> ignoré pour celui-ci.", evenement.getId(), evenement.getClass().getSimpleName()));
			}
			return;
		}

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();
			marshaller.marshal(objectFactory.createFiscalEvent(event), doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(String.valueOf(evenement.getId()));
			m.setBusinessUser(EvenementFiscalHelper.getBusinessUser(evenement.getLogCreationUser()));
			m.setServiceDestination(serviceDestination);
			m.setContext("fiscalEvent.v5");
			m.addHeader(VERSION_ATTRIBUTE, "5");

			if (evenement instanceof EvenementFiscalTiers) {
				m.addHeader("noCtb", String.valueOf(((EvenementFiscalTiers) evenement).getTiers().getNumero()));
			}

			m.setBody(doc);

			if (outputQueue != null) {
				m.setServiceDestination(outputQueue); // for testing only
			}

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un événement fiscal.";
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, message, e);

			throw new EvenementFiscalException(message, e);
		}
	}
}

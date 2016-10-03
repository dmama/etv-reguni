package ch.vd.uniregctb.evenement.docsortant;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.docsortant.v1.Documents;
import ch.vd.unireg.xml.event.docsortant.v1.ObjectFactory;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.utils.LogLevel;

public class EvenementDocumentSortantSenderImpl implements EvenementDocumentSortantSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDocumentSortantSenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private boolean enabled;

	private JAXBContext jaxbContext;

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

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEvenementDocumentSortant(String businessId, Documents events) throws EvenementDocumentSortantException {

		if (!enabled) {
			LOGGER.info(String.format("Evénements de notification des documents sortants désactivés : l'événement '%s' n'est pas envoyé.", businessId));
			return;
		}

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();
			marshaller.marshal(events, doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(businessId);
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setContext("documentSortant");
			m.setBody(doc);

			if (outputQueue != null) {
				m.setServiceDestination(outputQueue); // for testing only
			}

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un événement de document sortant.";
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, message, e);

			throw new EvenementDocumentSortantException(message, e);
		}
	}
}

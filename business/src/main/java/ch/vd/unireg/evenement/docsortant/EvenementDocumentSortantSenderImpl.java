package ch.vd.unireg.evenement.docsortant;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.utils.LogLevel;
import ch.vd.unireg.xml.event.docsortant.v1.Documents;
import ch.vd.unireg.xml.event.docsortant.v1.ObjectFactory;

public class EvenementDocumentSortantSenderImpl implements EvenementDocumentSortantSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDocumentSortantSenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private String serviceDestinationEnMasse;
	private String queueRetour;
	private boolean enabled;
	private boolean enabledEnMasse;

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

	public void setServiceDestinationEnMasse(String serviceDestinationEnMasse) {
		this.serviceDestinationEnMasse = serviceDestinationEnMasse;
	}

	public void setQueueRetour(String queueRetour) {
		this.queueRetour = queueRetour;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setEnabledEnMasse(boolean enabledEnMasse) {
		this.enabledEnMasse = enabledEnMasse;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEvenementDocumentSortant(String businessId, Documents events, boolean reponseAttendue, @Nullable Map<String, String> additionalHeaders, boolean local) throws EvenementDocumentSortantException {

		//La  queue d'envoi en traitement local est désactivée et on est en mode local on envoie rien
		if (!enabled && local) {
			LOGGER.info(String.format("Evénements de notification des documents sortants désactivés : l'événement '%s' n'est pas envoyé.", businessId));
			return;
		}

		//La  queue d'envoi en masse est désactivée et on est en mode bach (donc pas local )  on envoie rien
		if (!enabledEnMasse && !local) {
			LOGGER.info(String.format("Evénements de notification des documents sortants en masse désactivés : l'événement '%s' n'est pas envoyé.", businessId));
			return;
		}
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		if (principal == null) {
			throw new IllegalArgumentException();
		}

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
			if (local) {
				m.setServiceDestination(serviceDestination);
			}
			else{
				m.setServiceDestination(serviceDestinationEnMasse);
			}
			m.setContext("documentSortant");
			m.setBody(doc);

			// si on attend une réponse, il faut le dire et indiquer où on l'attend
			if (reponseAttendue) {
				m.setServiceReplyTo(queueRetour);
			}

			// et ajout des informations de headers demandeés
			if (additionalHeaders != null && !additionalHeaders.isEmpty()) {
				EsbMessageHelper.setHeaders(m, additionalHeaders, true);
			}

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

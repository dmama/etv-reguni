package ch.vd.unireg.evenement.ide;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0024.v3.ObjectFactory;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.utils.LogLevel;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public class AnnonceIDESenderImpl implements AnnonceIDESender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDESenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private String serviceReplyTo;
	private boolean enabled;

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

	public void setServiceReplyTo(String serviceReplyTo) {
		this.serviceReplyTo = serviceReplyTo;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}


	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void sendEvent(AnnonceIDEEnvoyee annonce, String msgBusinessId) throws AnnonceIDEException {
		if (annonce == null) {
			throw new IllegalArgumentException("Contenu de l'annonce manquant.");
		} else if (annonce.getNumero() == null) {
			throw new IllegalArgumentException("Numéro d'annonce manquant.");
		} else if (StringUtils.isBlank(msgBusinessId)) {
			throw new IllegalArgumentException("Contenu de l'annonce manquant.");
		}

		if (!enabled) {
			// On ne peut pas laisser la transaction se terminer normallement car cela conduirait à conserver une référence pour une annonce qui n'a pas été envoyée.
			throw new AnnonceIDEException(String.format("Emission des annonces à l'IDE désactivée par configuration : abandon de l'annonce n°%d.", annonce.getNumero()));
		}

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		NoticeRequest event = RCEntAnnonceIDEHelper.buildNoticeRequest(annonce);

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();
			marshaller.marshal(objectFactory.createNoticeRequest(event), doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(msgBusinessId);
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setServiceReplyTo(serviceReplyTo);
			m.setContext("demandeAnnonceIDE");
			m.setBody(doc);

			if (outputQueue != null) {
				m.setServiceDestination(outputQueue); // for testing only
			}

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'une annonce à l'IDE.";
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, message, e);

			throw new AnnonceIDEException(message, e);
		}
	}
}

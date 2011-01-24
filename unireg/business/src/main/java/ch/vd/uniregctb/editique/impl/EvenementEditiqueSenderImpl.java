package ch.vd.uniregctb.editique.impl;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.EvenementEditiqueSender;

/**
 * Bean qui permet d'envoyer des documents à l'éditique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementEditiqueSenderImpl implements EvenementEditiqueSender {

	private static final Logger LOGGER = Logger.getLogger(EvenementEditiqueSenderImpl.class);

	//private static final int MAX_PRIORITY = 9;

	private EsbJmsTemplate esbTemplate; // ESB template standard
	private EsbJmsTemplate noTxEsbTemplate; // ESB template non-rattaché au transaction manager
	private EsbMessageFactory esbMessageFactory;
	private String serviceDestination;
	private String serviceReplyTo;

	public String envoyerDocumentImmediatement(String nomDocument, String typeDocument, XmlObject document, TypeFormat typeFormat, boolean archive) throws EditiqueException {
		return envoyer(nomDocument, typeDocument, document, TypeImpression.DIRECT, typeFormat, archive, noTxEsbTemplate);
	}

	public String envoyerDocument(String nomDocument, String typeDocument, XmlObject document, TypeFormat typeFormat, boolean archive) throws EditiqueException {
		return envoyer(nomDocument, typeDocument, document, TypeImpression.BATCH, typeFormat, archive, esbTemplate);
	}

	private String envoyer(String nomDocument, String typeDocument, XmlObject document, TypeImpression typeImpression, TypeFormat typeFormat, boolean archive, EsbJmsTemplate esbTemplate) throws EditiqueException {

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		try {
			final EsbMessage m = esbMessageFactory.createMessage();
			
			// meta-info requis par l'ESB
			m.setBusinessId(nomDocument);
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);

			// pas de retour si on est en batch...
			if (TypeImpression.DIRECT.equals(typeImpression)) {
				m.setServiceReplyTo(serviceReplyTo);
			}

			// TODO (jde) il faudrait un nom plus spécifique à chaque type de document...
			m.setContext("evenementEditique");

			// meta-info requis par éditique
			m.addHeader(TypeMessagePropertiesNames.PRINT_MODE_MESSAGE_PROPERTY_NAME.toString(), typeImpression.toString());
			m.addHeader(TypeMessagePropertiesNames.ARCHIVE_MESSAGE_PROPERTY_FLAG.toString(), Boolean.toString(archive));
			m.addHeader(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString(), typeDocument);
			if (TypeImpression.DIRECT.equals(typeImpression)) {
				m.addHeader(TypeMessagePropertiesNames.RETURN_FORMAT_MESSAGE_PROPERTY_NAME.toString(), typeFormat.toString());
				// TODO (msi) demander à giampaolo comment setter cette valeur : message.setJMSPriority(MAX_PRIORITY);
				m.addHeader(EditiqueHelper.DI_ID, nomDocument);
			}

			// le document à imprimer lui-même
			final Node node = document.newDomNode();
			m.setBody((Document) node);

			// on envoie l'événement sous forme de message JMS à travers l'ESB
			esbTemplate.send(m);

			final String messageId = m.getMessageId();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Message ID JMS :" + messageId + "--");
				LOGGER.trace("ID :" + nomDocument + "--");
			}

			return messageId;
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un document au service Editique JMS";
			LOGGER.error(message, e);

			throw new EditiqueException(message, e);
		}
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setNoTxEsbTemplate(EsbJmsTemplate noTxEsbTemplate) {
		this.noTxEsbTemplate = noTxEsbTemplate;
	}

	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setServiceReplyTo(String serviceReplyTo) {
		this.serviceReplyTo = serviceReplyTo;
	}
}

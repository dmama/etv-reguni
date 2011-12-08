package ch.vd.uniregctb.editique.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.EvenementEditiqueSender;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

/**
 * Bean qui permet d'envoyer des documents à l'éditique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementEditiqueSenderImpl implements EvenementEditiqueSender {

	private static final Logger LOGGER = Logger.getLogger(EvenementEditiqueSenderImpl.class);

	private EsbJmsTemplate esbTemplate; // ESB template standard
	private EsbJmsTemplate noTxEsbTemplate; // ESB template non-rattaché au transaction manager
	private EsbMessageFactory esbMessageFactory;
	private String serviceDestination;
	private String serviceReplyTo;

	@Override
	public String envoyerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, TypeFormat typeFormat, boolean archive) throws EditiqueException {
		return envoyer(nomDocument, typeDocument, document, TypeImpression.DIRECT, typeFormat, archive, noTxEsbTemplate);
	}

	@Override
	public String envoyerDocument(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, TypeFormat typeFormat, boolean archive) throws EditiqueException {
		return envoyer(nomDocument, typeDocument, document, TypeImpression.BATCH, typeFormat, archive, esbTemplate);
	}

	private String envoyer(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, TypeImpression typeImpression, TypeFormat typeFormat, boolean archive, EsbJmsTemplate esbTemplate) throws EditiqueException {

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		// tant que les documents éditiques n'ont pas de namespace, ils ne peuvent pas être validés par le framework
		// de la message factory de l'ESB. D'après les guidelines de l'ESB, il faut donc les valider à la main...
		validate(document);

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

			m.setContext(typeDocument.getContexteImpression());

			// meta-info requis par éditique
			m.addHeader(TypeMessagePropertiesNames.PRINT_MODE_MESSAGE_PROPERTY_NAME.toString(), typeImpression.toString());
			m.addHeader(TypeMessagePropertiesNames.ARCHIVE_MESSAGE_PROPERTY_FLAG.toString(), Boolean.toString(archive));
			m.addHeader(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString(), typeDocument.getCodeDocumentEditique());
			if (TypeImpression.DIRECT.equals(typeImpression)) {
				m.addHeader(TypeMessagePropertiesNames.RETURN_FORMAT_MESSAGE_PROPERTY_NAME.toString(), typeFormat.toString());
				m.addHeader(EditiqueHelper.DI_ID, nomDocument);
			}

			// le document à imprimer lui-même
			m.setBody(XmlUtils.xmlbeans2string(document));

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

	private static void validate(XmlObject document) {

		// Endroit où on va récupérer les éventuelles erreurs
		final XmlOptions validateOptions = new XmlOptions();
		final List<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);

		// C'est parti pour la validation !
		final boolean isValid = document.validate(validateOptions);

		// si le document n'est pas valide, on va logguer pour avoir de quoi identifier et corriger le bug ensuite
		if (!isValid) {
			final StringBuilder b = new StringBuilder();
			b.append("--------------------------------------------------\n");
			b.append("--------------------------------------------------\n");
			b.append("Erreur de validation du message éditique en sortie\n");
			b.append("--------------------------------------------------\n");
			b.append("Message :\n").append(document).append('\n');
			b.append("--------------------------------------------------\n");
			for (XmlError error : errorList) {
				b.append("Erreur : ").append(error.getMessage()).append('\n');
				b.append("Localisation de l'erreur : ").append(error.getCursorLocation().xmlText()).append('\n');
				b.append("--------------------------------------------------\n");
			}
			b.append("--------------------------------------------------\n");
			Assert.fail(b.toString());
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

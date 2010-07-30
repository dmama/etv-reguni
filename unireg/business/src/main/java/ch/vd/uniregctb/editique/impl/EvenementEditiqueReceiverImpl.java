package ch.vd.uniregctb.editique.impl;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.jms.core.JmsTemplate;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EvenementEditiqueReceiver;

/**
 * Listener qui reçoit les messages JMS concernant les événements éditique, les valide, les transforme et les transmet au handler approprié.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementEditiqueReceiverImpl implements EvenementEditiqueReceiver {

	private static final Logger LOGGER = Logger.getLogger(EvenementEditiqueReceiverImpl.class);

	private static final String PDF_MIME = "application/pdf";
	private static final String PCL_MIME = "application/x-pcl";
	private static final String TIF_MIME = "image/tiff";
	private static final String AFP_MIME = "application/afp";
	private static final String DEFAULT_ATTACHEMENT_NAME = "data";

	private EsbJmsTemplate noTxEsbTemplate; // ESB template non-rattaché au transaction manager
	private String destinationName;

	/**
	 * Temps d'attente (en secondes) du retour du document PDF / PCL lors d'une impression locale.
	 */
	private int receiveTimeout = 120;

	public EditiqueResultat getDocument(TypeFormat typeFormat, String nomDocument, boolean appliqueDelai) throws Exception {

		final long timeout = (appliqueDelai ? receiveTimeout * 1000 : JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
		noTxEsbTemplate.setReceiveTimeout(timeout);

		// On n'extrait de la queue que le message demandé
		final EsbMessage message = noTxEsbTemplate.receiveSelected(destinationName, EditiqueHelper.DI_ID + " = '" + nomDocument + "'");
		if (message == null) {
			return null;
		}

		return createResultfromMessage(message, typeFormat);
	}

	/**
	 * Créer la réponse avec les informations contenues dans le message.
	 *
	 * @param message    message JMS
	 * @param typeFormat le format du document
	 * @return Retourne un réponse
	 * @throws JMSException arrive quand survient une erreur JMS.
	 */
	private EditiqueResultat createResultfromMessage(EsbMessage message, TypeFormat typeFormat) throws Exception {

		final EditiqueResultatImpl resultat = new EditiqueResultatImpl();
		resultat.setTimestampRecieved(System.currentTimeMillis());

		final byte[] buffer = message.getAttachmentAsByteArray(DEFAULT_ATTACHEMENT_NAME);
		final String documentType = message.getHeader(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString());
		final String idDocument = message.getHeader(EditiqueHelper.DI_ID);
		final String error = message.getHeader(TypeMessagePropertiesNames.ERROR_MESSAGE_PROPERTY_NAME.toString());

		resultat.setDocument(buffer);
		resultat.setDocumentType(documentType);
		resultat.setIdDocument(idDocument);
		resultat.setContentType(getContentType(typeFormat));
		resultat.setError(error);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(resultat.toString());
		}

		return resultat;
	}

	private static String getContentType(TypeFormat typeFormat) {
		if (TypeFormat.PDF.equals(typeFormat)) {
			return PDF_MIME;
		}
		else if (TypeFormat.PCL.equals(typeFormat)) {
			return PCL_MIME;
		}
		else if (TypeFormat.TIF.equals(typeFormat)) {
			return TIF_MIME;
		}
		else if (TypeFormat.AFP.equals(typeFormat)) {
			return AFP_MIME;
		}
		else {
			throw new RuntimeException("TypeFormat non supporté : " + typeFormat);
		}
	}

	public void setNoTxEsbTemplate(EsbJmsTemplate noTxEsbTemplate) {
		this.noTxEsbTemplate = noTxEsbTemplate;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}
}
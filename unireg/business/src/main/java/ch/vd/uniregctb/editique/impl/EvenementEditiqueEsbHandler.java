package ch.vd.uniregctb.editique.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueResultatRecu;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;
import ch.vd.uniregctb.jms.EsbMessageHandler;

/**
 * Listener des retours d'impression éditique
 */
public class EvenementEditiqueEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEditiqueEsbHandler.class);

	private static final String DEFAULT_ATTACHEMENT_NAME = "data";

	private static final Map<String, String> mimeTypes;
	static {
		mimeTypes = new HashMap<>();
		mimeTypes.put("pcl", MimeTypeHelper.MIME_XPCL);
		mimeTypes.put("pdf", MimeTypeHelper.MIME_PDF);
		mimeTypes.put("tiff", MimeTypeHelper.MIME_TIFF);
		mimeTypes.put("afp", MimeTypeHelper.MIME_AFP);
	}

	private EditiqueRetourImpressionStorageService storageService;

	public void setStorageService(EditiqueRetourImpressionStorageService storageService) {
		this.storageService = storageService;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		final String idDocument = message.getHeader(ConstantesEditique.UNIREG_DOCUMENT_ID);
		LOGGER.info(String.format("Arrivée d'un retour éditique pour le document '%s'", idDocument));

		try {
			final EditiqueResultatRecu document = createResultfromMessage(message);
			if (document != null) {
				storageService.onArriveeRetourImpression(document);
			}
			else {
				LOGGER.warn("Impossible de reconnaître le document retourné");
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Créer la réponse avec les informations contenues dans le message.
	 *
	 * @param message message JMS
	 * @return les données du document imprimé
	 */
	private EditiqueResultatRecu createResultfromMessage(EsbMessage message) throws IOException {

		final EditiqueResultatRecu resultat;
		final String idDocument = message.getHeader(ConstantesEditique.UNIREG_DOCUMENT_ID);
		final String error = message.getHeader(ConstantesEditique.ERROR_MESSAGE);
		if (StringUtils.isNotBlank(error)) {
			final ErrorType errorType = ErrorType.valueOf(message.getHeader(EsbMessage.ERROR_TYPE));
			final String errorCode = message.getHeader(EsbMessage.ERROR_CODE);
			resultat = new EditiqueResultatErreurImpl(idDocument, error, errorType, errorCode);
		}
		else {
			final byte[] buffer;
			final Set<String> attachmentNames = message.getAttachmentsNames();
			if (attachmentNames == null || attachmentNames.isEmpty()) {
				buffer = null;
			}
			else {
				final String attachmentName;
				if (attachmentNames.size() > 1) {
					// je ne sais pas lequel prendre... essayons avec le nom par défaut
					attachmentName = DEFAULT_ATTACHEMENT_NAME;
				}
				else {
					attachmentName = attachmentNames.iterator().next();
				}
				buffer = message.getAttachmentAsByteArray(attachmentName);
			}

			final String documentType = message.getHeader(ConstantesEditique.UNIREG_TYPE_DOCUMENT);
			final String returnFormat = message.getHeader(ConstantesEditique.UNIREG_FORMAT_DOCUMENT);
			final String mimeType = mimeTypes.get(returnFormat);

			resultat = new EditiqueResultatDocumentImpl(idDocument, mimeType, documentType, buffer);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Retour d'impression :\n%s", resultat.toString()));
		}

		return resultat;
	}
}

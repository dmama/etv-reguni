package ch.vd.uniregctb.editique.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		final String idDocument = message.getHeader(ConstantesEditique.DOCUMENT_ID);
		LOGGER.info(String.format("Arrivée d'un retour d'impression pour le document '%s'", idDocument));

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
		final String idDocument = message.getHeader(ConstantesEditique.DOCUMENT_ID);
		final String error = message.getHeader(ConstantesEditique.ERROR_MESSAGE);
		if (StringUtils.isNotBlank(error)) {
			resultat = new EditiqueResultatErreurImpl(idDocument, error);
		}
		else {
			final byte[] buffer = message.getAttachmentAsByteArray(DEFAULT_ATTACHEMENT_NAME);
			final String documentType = message.getHeader(ConstantesEditique.DOCUMENT_TYPE);
			final String returnFormat = message.getHeader(ConstantesEditique.RETURN_FORMAT);
			final String mimeType = mimeTypes.get(returnFormat);

			resultat = new EditiqueResultatDocumentImpl(idDocument, mimeType, documentType, buffer);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Retour d'impression :\n%s", resultat.toString()));
		}

		return resultat;
	}
}

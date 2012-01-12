package ch.vd.uniregctb.editique.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.EditiqueResultatRecu;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

/**
 * Listener des retours d'impression éditique
 */
public class EvenementEditiqueListenerImpl extends EsbMessageEndpointListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementEditiqueListenerImpl.class);

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	private static final String DEFAULT_ATTACHEMENT_NAME = "data";

	private static final Map<String, String> mimeTypes;
	static {
		mimeTypes = new HashMap<String, String>();
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

		nbMessagesRecus.incrementAndGet();

		final String idDocument = message.getHeader(EditiqueHelper.DI_ID);
		LOGGER.info(String.format("Arrivée d'un retour d'impression pour le document '%s'", idDocument));

		final EditiqueResultatRecu document = createResultfromMessage(message);
		if (document != null) {
			storageService.onArriveeRetourImpression(document);
		}
		else {
			LOGGER.warn("Impossible de reconnaître le document retourné");
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
		final long timestampReceived = TimeHelper.getPreciseCurrentTimeMillis();
		final String idDocument = message.getHeader(EditiqueHelper.DI_ID);
		final String error = message.getHeader(TypeMessagePropertiesNames.ERROR_MESSAGE_PROPERTY_NAME.toString());
		if (StringUtils.isNotBlank(error)) {
			resultat = new EditiqueResultatErreurImpl(idDocument, error, timestampReceived);
		}
		else {
			final byte[] buffer = message.getAttachmentAsByteArray(DEFAULT_ATTACHEMENT_NAME);
			final String documentType = message.getHeader(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString());
			final String returnFormat = message.getHeader(TypeMessagePropertiesNames.RETURN_FORMAT_MESSAGE_PROPERTY_NAME.toString());
			final String mimeType = mimeTypes.get(returnFormat);

			resultat = new EditiqueResultatDocumentImpl(idDocument, mimeType, documentType, buffer, timestampReceived);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Retour d'impression :\n%s", resultat.toString()));
		}

		return resultat;
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}

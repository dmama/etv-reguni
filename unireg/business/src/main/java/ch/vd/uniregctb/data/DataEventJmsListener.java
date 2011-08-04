package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument.DataChangeEvent;
import ch.vd.fiscalite.registre.databaseEvent.DataType;
import ch.vd.fiscalite.registre.databaseEvent.DataType.Enum;
import ch.vd.fiscalite.registre.databaseEvent.DatabaseLoadEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DatabaseTruncateEventDocument;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

/**
 * Bean qui écoute les messages JMS de modification de la database pour propager l'information au database service
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataEventJmsListener extends EsbMessageListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(DataEventJmsListener.class);

	private DataEventService dataEventService;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		nbMessagesRecus.incrementAndGet();

		// Parse le message sous forme XML
		final XmlObject doc = XmlObject.Factory.parse(msg.getBodyAsString());

		// Valide le bousin
		final XmlOptions validateOptions = new XmlOptions();
		final List<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!doc.validate(validateOptions)) {
			final StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
			}

			final String errorMsg = builder.toString();
			LOGGER.error(errorMsg);
			getEsbTemplate().sendError(msg, errorMsg, null, ErrorType.TECHNICAL, "");
		}
		else {

			// Traite le message
			AuthenticationHelper.pushPrincipal("JMS-DbEvent(" + msg.getMessageId() + ")");
			try {

				if (doc instanceof DataChangeEventDocument) {
					onDataChange((DataChangeEventDocument) doc);
				}
				else if (doc instanceof DatabaseLoadEventDocument) {
					onDatabaseLoad();
				}
				else if (doc instanceof DatabaseTruncateEventDocument) {
					onDatabaseTruncate();
				}
				else {
					LOGGER.error("Type de message inconnu : " + doc.getClass().getName());
				}
			}
			catch (Exception e) {
				LOGGER.error("Erreur lors de la réception du message n°" + msg.getMessageId(), e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	private void onDataChange(DataChangeEventDocument changeDoc) {

		final DataChangeEvent event = changeDoc.getDataChangeEvent();
		final long id = event.getId();
		final Enum type = event.getType();

		if (DataType.TIERS.equals(type)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur le tiers n°" + id);
			}
			dataEventService.onTiersChange(id);
		}
		if (DataType.INDIVIDU.equals(type)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur l'individu n°" + id);
			}
			dataEventService.onIndividuChange(id);
		}
		if (DataType.PM.equals(type)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur la PM n°" + id);
			}
			dataEventService.onPersonneMoraleChange(id);
		}
		else if (DataType.DROIT_ACCES.equals(type)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Réception d'un événement db de changement sur les droits d'accès du tiers n°" + id);
			}
			dataEventService.onDroitAccessChange(id);
		}
	}

	private void onDatabaseLoad() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Réception d'un événement de chargement de la database");
		}
		dataEventService.onLoadDatabase();
	}

	private void onDatabaseTruncate() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Réception d'un événement de truncate de la database");
		}
		dataEventService.onTruncateDatabase();
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}

package ch.vd.uniregctb.evenement.database;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;

import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DataType;
import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument.DataChangeEvent;
import ch.vd.fiscalite.registre.databaseEvent.DataType.Enum;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.database.DatabaseService;

/**
 * Bean qui écoute les messages JMS de modification de la database pour propager l'information au database service
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DatabaseEventListener extends EsbMessageListener {

	private static Logger LOGGER = Logger.getLogger(DatabaseEventListener.class);

	private DatabaseService databaseService;

	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		// Parse le message sous forme XML
		final DataChangeEventDocument doc = DataChangeEventDocument.Factory.parse(msg.getBodyAsString());

		// Valide le bousin
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!doc.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
			}
			throw new RuntimeException(builder.toString());
		}

		// Handle le message
		final DataChangeEvent event = doc.getDataChangeEvent();
		final long id = event.getId();
		final Enum type = event.getType();
		try {
			AuthenticationHelper.pushPrincipal("JMS-DbEvent(" + msg.getMessageId() + ")");
			if (DataType.TIERS.equals(type)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Réception d'un événement db de changement sur le tiers n°" + id);
				}
				databaseService.onTiersChange(id);
			}
			else if (DataType.DROIT_ACCES.equals(type)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Réception d'un événement db de changement sur les droits d'accès du tiers n°" + id);
				}
				databaseService.onDroitAccessChange(id);
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}

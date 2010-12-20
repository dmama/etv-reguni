package ch.vd.uniregctb.pm;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import ch.vd.fiscalite.registre.entrepriseEvent.EvtEntrepriseDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

/**
 * Bean qui écoute les messages JMS en provenance du registre des entreprises (PMs).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EntrepriseEventListener extends EsbMessageListener implements MonitorableMessageListener {

	private static Logger LOGGER = Logger.getLogger(EntrepriseEventListener.class);

	private GlobalTiersIndexer indexer;
	private DataEventService dataEventService;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		nbMessagesRecus.incrementAndGet();

		// Parse le message sous forme XML
		final XmlObject doc = XmlObject.Factory.parse(msg.getBodyAsString());

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
		AuthenticationHelper.pushPrincipal("JMS-PmEvent(" + msg.getMessageId() + ")");
		try {

			if (doc instanceof EvtEntrepriseDocument) {
				onEvtEntreprise((EvtEntrepriseDocument) doc);
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

	private void onEvtEntreprise(EvtEntrepriseDocument doc) {
		final EvtEntrepriseDocument.EvtEntreprise event = doc.getEvtEntreprise();
		final int entrepriseId = event.getNoEntreprise();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Arrivée d'un événement sur la PM n°" + entrepriseId);
		}
		indexer.schedule(entrepriseId);
		dataEventService.onTiersChange(entrepriseId);
	}

	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}

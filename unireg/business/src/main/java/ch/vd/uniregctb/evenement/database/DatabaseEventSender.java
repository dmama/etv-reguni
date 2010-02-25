package ch.vd.uniregctb.evenement.database;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DataType;
import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument.DataChangeEvent;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.database.DatabaseListener;
import ch.vd.uniregctb.database.DatabaseService;

/**
 * Bean qui envoie les événements JSM de modification de la database.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DatabaseEventSender implements DatabaseListener, InitializingBean {

	private static Logger LOGGER = Logger.getLogger(DatabaseEventSender.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private DatabaseService databaseService;
	private String serviceDestination;
	private String businessUser;

	/**
	 * for testing purpose
	 */
	protected void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	public void afterPropertiesSet() throws Exception {
		databaseService.register(this);
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onDroitAccessChange(long tiersId) {
		try {
			sendEvent(tiersId, TiersType.DROIT_ACCES);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement de droit d'accès sur le tiers n°" + tiersId, e);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onTiersChange(long id) {
		try {
			sendEvent(id, TiersType.TIERS);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement du tiers n°" + id, e);
		}
	}

	public void onLoadDatabase() {
		// rien à faire
	}

	public void onTruncateDatabase() {
		// rien à faire
	}

	private enum TiersType {
		TIERS, DROIT_ACCES
	}

	private void sendEvent(long id, TiersType type) throws Exception {

		final DataChangeEventDocument doc = DataChangeEventDocument.Factory.newInstance();

		final DataChangeEvent event = doc.addNewDataChangeEvent();
		event.setId(id);
		switch (type) {
		case TIERS:
			event.setType(DataType.TIERS);
			break;
		case DROIT_ACCES:
			event.setType(DataType.DROIT_ACCES);
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(id));
		m.setBusinessUser(businessUser);
		m.setBusinessCorrelationId(String.valueOf(id));
		m.setServiceDestination(serviceDestination);
		m.setDomain("fiscalite");
		m.setContext("databaseEvent");
		m.setApplication("unireg");
		final Node node = doc.newDomNode();
		m.setBody((Document) node);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}

}

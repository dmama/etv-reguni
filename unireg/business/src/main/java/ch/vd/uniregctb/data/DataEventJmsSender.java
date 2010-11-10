package ch.vd.uniregctb.data;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument.DataChangeEvent;
import ch.vd.fiscalite.registre.databaseEvent.DatabaseLoadEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DatabaseTruncateEventDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.EsbMessageImpl;
import ch.vd.technical.esb.jms.EsbJmsTemplate;

/**
 * Bean qui envoie les événements de modification de données comme messages JSM.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataEventJmsSender implements DataEventListener, InitializingBean {

	private static Logger LOGGER = Logger.getLogger(DataEventJmsSender.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private DataEventService dataEventService;
	private String serviceDestination;
	private String businessUser;

	/**
	 * for testing purpose
	 */
	@SuppressWarnings({"JavaDoc", "UnusedDeclaration"})
	protected void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onDroitAccessChange(long tiersId) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement sur les droits d'accès du tiers n°" + tiersId);
			}
			sendChangeEvent(tiersId, DataType.DROIT_ACCES);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement de droit d'accès sur le tiers n°" + tiersId, e);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onTiersChange(long id) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement sur le tiers n°" + id);
			}
			sendChangeEvent(id, DataType.TIERS);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement du tiers n°" + id, e);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onIndividuChange(long id) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement sur l'individu n°" + id);
			}
			sendChangeEvent(id, DataType.INDIVIDU);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement du tiers n°" + id, e);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onLoadDatabase() {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement de chargement de la database");
			}
			sendLoadEvent();
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de load de la DB", e);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public void onTruncateDatabase() {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement de truncate de la database");
			}
			sendTruncateEvent();
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de truncate de la DB", e);
		}
	}

	private enum DataType {
		TIERS,
		INDIVIDU,
		DROIT_ACCES
	}

	private void sendChangeEvent(long id, DataType type) throws Exception {

		final DataChangeEventDocument doc = DataChangeEventDocument.Factory.newInstance();

		final DataChangeEvent event = doc.addNewDataChangeEvent();
		event.setId(id);
		switch (type) {
		case TIERS:
			event.setType(ch.vd.fiscalite.registre.databaseEvent.DataType.TIERS);
			break;
		case INDIVIDU:
			event.setType(ch.vd.fiscalite.registre.databaseEvent.DataType.INDIVIDU);
			break;
		case DROIT_ACCES:
			event.setType(ch.vd.fiscalite.registre.databaseEvent.DataType.DROIT_ACCES);
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}

		final EsbMessageImpl m = (EsbMessageImpl) esbMessageFactory.createMessage();
		m.setValidator(null); // [UNIREG-2399] on ne veut pas valider les événements DB parce que ça prend 60% du temps d'insertion des événements civils
		m.setBusinessId(String.valueOf(id));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		final Node node = doc.newDomNode();
		m.setBody((Document) node);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}

	private void sendTruncateEvent() throws Exception {

		final DatabaseTruncateEventDocument doc = DatabaseTruncateEventDocument.Factory.newInstance();
		doc.addNewDatabaseTruncateEvent();

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		final Node node = doc.newDomNode();
		m.setBody((Document) node);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}

	private void sendLoadEvent() throws Exception {

		final DatabaseLoadEventDocument doc = DatabaseLoadEventDocument.Factory.newInstance();
		doc.addNewDatabaseLoadEvent();

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		final Node node = doc.newDomNode();
		m.setBody((Document) node);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}
}

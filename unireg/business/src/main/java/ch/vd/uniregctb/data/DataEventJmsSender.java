package ch.vd.uniregctb.data;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DataChangeEventDocument.DataChangeEvent;
import ch.vd.fiscalite.registre.databaseEvent.DatabaseLoadEventDocument;
import ch.vd.fiscalite.registre.databaseEvent.DatabaseTruncateEventDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.XmlUtils;

/**
 * Bean qui envoie les événements de modification de données comme messages JSM.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataEventJmsSender implements DataEventListener, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(DataEventJmsSender.class);

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

	@Override
	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
	}

	@Override
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

	@Override
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

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void onIndividuChange(long id) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement sur l'individu n°" + id);
			}
			sendChangeEvent(id, DataType.INDIVIDU);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement de l'individu n°" + id, e);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void onPersonneMoraleChange(long id) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement sur la PM n°" + id);
			}
			sendChangeEvent(id, DataType.PM);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement de la PM n°" + id, e);
		}
	}

	@Override
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

	@Override
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
		PM,
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
		case PM:
			event.setType(ch.vd.fiscalite.registre.databaseEvent.DataType.PM);
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + ']');
		}

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(id));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		m.setBody(XmlUtils.xmlbeans2string(doc));

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		send(m);
	}

	private void sendTruncateEvent() throws Exception {

		final DatabaseTruncateEventDocument doc = DatabaseTruncateEventDocument.Factory.newInstance();
		doc.addNewDatabaseTruncateEvent();

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		m.setBody(XmlUtils.xmlbeans2string(doc));

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		send(m);
	}

	private void sendLoadEvent() throws Exception {

		final DatabaseLoadEventDocument doc = DatabaseLoadEventDocument.Factory.newInstance();
		doc.addNewDatabaseLoadEvent();

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		m.setBody(XmlUtils.xmlbeans2string(doc));

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		send(m);
	}

	private void send(EsbMessage m) throws Exception {
		esbTemplate.sendInternal(m); // [UNIREG-3242] utilisation d'une queue interne
	}
}

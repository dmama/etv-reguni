package ch.vd.uniregctb.data;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseLoadEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseTruncateEvent;
import ch.vd.unireg.xml.event.data.v1.DroitAccesChangeEvent;
import ch.vd.unireg.xml.event.data.v1.IndividuChangeEvent;
import ch.vd.unireg.xml.event.data.v1.ObjectFactory;
import ch.vd.unireg.xml.event.data.v1.PmChangeEvent;
import ch.vd.unireg.xml.event.data.v1.RelationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.Relationship;
import ch.vd.unireg.xml.event.data.v1.TiersChangeEvent;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Bean qui envoie les événements de modification de données comme messages JSM.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataEventJmsSender implements DataEventListener, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataEventJmsSender.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private DataEventService dataEventService;
	private String serviceDestination;
	private String businessUser;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private JAXBContext jaxbContext;

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
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void onDroitAccessChange(long tiersId) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement sur les droits d'accès du tiers n°" + tiersId);
			}

			final DroitAccesChangeEvent event = objectFactory.createDroitAccesChangeEvent();
			event.setId(tiersId);
			sendDataEvent(String.valueOf(tiersId), event);
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

			final TiersChangeEvent event = objectFactory.createTiersChangeEvent();
			event.setId(id);
			sendDataEvent(String.valueOf(id), event);
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

			final IndividuChangeEvent event = objectFactory.createIndividuChangeEvent();
			event.setId(id);
			sendDataEvent(String.valueOf(id), event);
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

			final PmChangeEvent event = objectFactory.createPmChangeEvent();
			event.setId(id);
			sendDataEvent(String.valueOf(id), event);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de changement de la PM n°" + id, e);
		}
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement db de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId);
			}

			final Relationship relationship = getRelationshipMapping(type);

			final RelationChangeEvent event = objectFactory.createRelationChangeEvent();
			event.setRelationType(relationship);
			event.setSujetId(sujetId);
			event.setObjetId(objetId);
			sendDataEvent(String.valueOf(event.hashCode()), event);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de load de la DB", e);
		}
	}

	/**Retourne le type au format DataEvent de relation correspondant au rapport entre tiers passé en paramètre
	 *
	 * @param type de rapport entre tiers
	 * @return la correspondance au type passé en paramètre
	 */
	protected static Relationship getRelationshipMapping(TypeRapportEntreTiers type) {
		final Relationship relationship;
		switch (type) {
			case ANNULE_ET_REMPLACE:
				relationship = Relationship.ANNULE_ET_REMPLACE;
				break;
			case APPARTENANCE_MENAGE:
				relationship = Relationship.APPARTENANCE_MENAGE;
				break;
			case CONSEIL_LEGAL:
				relationship = Relationship.CONSEIL_LEGAL;
				break;
			case CONTACT_IMPOT_SOURCE:
				relationship = Relationship.CONTACT_IMPOT_SOURCE;
				break;
			case CURATELLE:
				relationship = Relationship.CURATELLE;
				break;
			case PARENTE:
				relationship = Relationship.PARENTE;
				break;
			case PRESTATION_IMPOSABLE:
				relationship = Relationship.PRESTATION_IMPOSABLE;
				break;
			case REPRESENTATION:
				relationship = Relationship.REPRESENTATION;
				break;
			case TUTELLE:
				relationship = Relationship.TUTELLE;
				break;
			case ASSUJETTISSEMENT_PAR_SUBSTITUTION:
				relationship = Relationship.ASSUJETTISSEMENT_PAR_SUBSTITUTION;
				break;
			case ACTIVITE_ECONOMIQUE:
				relationship = Relationship.ACTIVITE_ECONOMIQUE;
				break;
			default:
				throw new IllegalArgumentException("Type de relation inconnu = [" + type + ']');
		}
		return relationship;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void onLoadDatabase() {
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement de chargement de la database");
			}

			final DatabaseLoadEvent event = objectFactory.createDatabaseLoadEvent();
			sendDataEvent(String.valueOf(event.hashCode()), event);
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

			final DatabaseTruncateEvent event = objectFactory.createDatabaseTruncateEvent();
			sendDataEvent(String.valueOf(event.hashCode()), event);
		}
		catch (Exception e) {
			LOGGER.error("Impossible d'envoyer un message de truncate de la DB", e);
		}
	}

	private void sendDataEvent(String businessId, DataEvent event) throws Exception {
		final Marshaller marshaller = jaxbContext.createMarshaller();
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();
		marshaller.marshal(objectFactory.createEvent(event), doc);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessId(businessId);
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("databaseEvent");
		m.setBody(doc);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		send(m);

		// Note : code pour unmarshaller un événement
		//		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		//		Unmarshaller u = context.createUnmarshaller();
		//		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		//		Schema schema = sf.newSchema(new File("mon_beau_xsd.xsd"));
		//		u.setSchema(schema);
		//		JAXBElement element = (JAXBElement) u.unmarshal(message);
		//		evenement = element == null ? null : (EvenementDeclarationImpot) element.getValue();
	}

	private void send(EsbMessage m) throws Exception {
		esbTemplate.sendInternal(m); // [UNIREG-3242] utilisation d'une queue interne
	}
}

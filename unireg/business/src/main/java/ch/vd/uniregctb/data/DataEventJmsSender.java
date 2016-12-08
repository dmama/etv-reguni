package ch.vd.uniregctb.data;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import ch.vd.unireg.xml.event.data.v1.OrganisationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.RelationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.Relationship;
import ch.vd.unireg.xml.event.data.v1.TiersChangeEvent;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Bean qui envoie les événements de modification de données comme messages JMS.
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
	 * Container des données déjà émises dans la transaction courante
	 * (histoire ne ne pas envoyer plusieurs messages identiques dans une même transaction)
	 */
	private final ThreadLocal<AlreadySentData> alreadySent = ThreadLocal.withInitial(AlreadySentData::new);

	/**
	 * Clé d'identification d'une relation entre tiers
	 */
	private static final class RelationshipKey implements Serializable {

		private static final long serialVersionUID = -6472419012587666128L;

		public final TypeRapportEntreTiers type;
		public final long sujetId;
		public final long objetId;

		private RelationshipKey(TypeRapportEntreTiers type, long sujetId, long objetId) {
			this.type = type;
			this.sujetId = sujetId;
			this.objetId = objetId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final RelationshipKey that = (RelationshipKey) o;
			return objetId == that.objetId && sujetId == that.sujetId && type == that.type;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + (int) (sujetId ^ (sujetId >>> 32));
			result = 31 * result + (int) (objetId ^ (objetId >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "RelationshipKey{" +
					"type=" + type +
					", sujetId=" + sujetId +
					", objetId=" + objetId +
					'}';
		}
	}

	/**
	 * Ids des entités changées pour lesquelles une notification a déjà été envoyée dans la transaction courante
	 * (comme ces notifications ne sont de toute façon envoyées qu'à la fin de la transaction, rien de sert
	 * de l'envoyer plusieurs fois...)
	 */
	private class AlreadySentData {

		private final Set<Long> tiersChange = new HashSet<>();
		private final Set<Long> individuChange = new HashSet<>();
		private final Set<Long> organisationChange = new HashSet<>();
		private final Set<Long> droitsAccesChange = new HashSet<>();
		private final Set<RelationshipKey> relationshipChange = new HashSet<>();

		public AlreadySentData() {
			// cleanup une fois la transaction terminée
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCompletion(int status) {
					// [SIFISC-22355][SIFISC-22258] un appel à "clear()" ne suffit pas ici, car la structure Data reste dans le ThreadLocal
					// et dans le cas de ré-utilisation du même thread plus tard (pool ?), on ne repasse plus par ce constructeur
					// et la structure ne sera plus nettoyée en fin de transaction parce qu'il manque alors l'enregistrement dans la transaction courante
					// -> on efface complètement la structure dans le ThreadLocal, ce qui forcera systématiquement un nouvel appel au constructeur
					alreadySent.remove();
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Cache des événements DB déjà envoyés dans la transaction nettoyé (cause : " + status + ")");
					}
				}
			});
		}

		/**
		 * @param id identifiant du tiers modifié
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme tiers modifié
		 */
		public boolean addTiersChange(Long id) {
			return tiersChange.add(id);
		}

		/**
		 * @param id identifiant de l'individu modifié
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme individu modifié
		 */
		public boolean addIndividuChange(Long id) {
			return individuChange.add(id);
		}

		/**
		 * @param id identifiant de l'organisation modifiée
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme organisation modifiée
		 */
		public boolean addOrganisationChange(Long id) {
			return organisationChange.add(id);
		}

		/**
		 * @param id identifiant du tiers dont les droits d'accès ont été modifiés
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme tiers modifié
		 */
		public boolean addDroitAccesChange(Long id) {
			return droitsAccesChange.add(id);
		}

		/**
		 * @param key identifiant de la relation entre tiers modifiée
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme relation modifiée
		 */
		public boolean addRelationshipChange(RelationshipKey key) {
			return relationshipChange.add(key);
		}
	}

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

	@FunctionalInterface
	private interface OnNotificationAction {
		/**
		 * @param data les données maintenues
		 * @return <code>true</code> s'il s'agit d'un nouvel enregistrement (= première fois que l'on voit cette notification)
		 */
		boolean registerNotification(AlreadySentData data);
	}

	/**
	 * @param action action sur les données maintenues
	 * @return la valeur de retour de l'action
	 */
	private boolean onNewNotification(OnNotificationAction action) {
		final AlreadySentData data = alreadySent.get();
		return action.registerNotification(data);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	public void onDroitAccessChange(final long tiersId) {
		final OnNotificationAction action = data -> data.addDroitAccesChange(tiersId);
		if (onNewNotification(action)) {
			try {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur les droits d'accès du tiers n°" + tiersId);
				}

				final DroitAccesChangeEvent event = objectFactory.createDroitAccesChangeEvent();
				event.setId(tiersId);
				sendDataEvent(String.valueOf(tiersId), event);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer un événement DB de changement de droit d'accès sur le tiers n°" + tiersId, e);
			}
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur les droits d'accès du tiers n°" + tiersId + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	public void onTiersChange(final long id) {
		final OnNotificationAction action = data -> data.addTiersChange(id);
		if (onNewNotification(action)) {
			try {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur le tiers n°" + id);
				}

				final TiersChangeEvent event = objectFactory.createTiersChangeEvent();
				event.setId(id);
				sendDataEvent(String.valueOf(id), event);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer un événement DB de changement du tiers n°" + id, e);
			}
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur le tiers n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	public void onOrganisationChange(final long id) {
		final OnNotificationAction action = data -> data.addOrganisationChange(id);
		if (onNewNotification(action)) {
			try {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur l'organisation n°" + id);
				}

				final OrganisationChangeEvent event = objectFactory.createOrganisationChangeEvent();
				event.setId(id);
				sendDataEvent(String.valueOf(id), event);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer un événement DB de changement de l'organisation n°" + id, e);
			}
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'organisation n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	public void onIndividuChange(final long id) {
		final OnNotificationAction action = data -> data.addIndividuChange(id);
		if (onNewNotification(action)) {
			try {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur l'individu n°" + id);
				}

				final IndividuChangeEvent event = objectFactory.createIndividuChangeEvent();
				event.setId(id);
				sendDataEvent(String.valueOf(id), event);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer un événement DB de changement de l'individu n°" + id, e);
			}
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'individu n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		final RelationshipKey key = new RelationshipKey(type, sujetId, objetId);
		final OnNotificationAction action = data -> data.addRelationshipChange(key);
		if (onNewNotification(action)) {
			try {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId);
				}

				final Relationship relationship = getRelationshipMapping(type);

				final RelationChangeEvent event = objectFactory.createRelationChangeEvent();
				event.setRelationType(relationship);
				event.setSujetId(sujetId);
				event.setObjetId(objetId);
				sendDataEvent(String.valueOf(event.hashCode()), event);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer événement DB de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId, e);
			}
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId + " (une émission est déjà prévue dans la transaction courante)");
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
			case MANDAT:
				relationship = Relationship.MANDAT;
				break;
			case FUSION_ENTREPRISES:
				relationship = Relationship.FUSION_ENTREPRISES;
				break;
			case ADMINISTRATION_ENTREPRISE:
				relationship = Relationship.ADMINISTRATION_ENTREPRISE;
				break;
			case SOCIETE_DIRECTION:
				relationship = Relationship.SOCIETE_DIRECTION;
				break;
			case SCISSION_ENTREPRISE:
				relationship = Relationship.SCISSION_ENTREPRISE;
				break;
			case TRANSFERT_PATRIMOINE:
				relationship = Relationship.TRANSFERT_PATRIMOINE;
				break;
			default:
				throw new IllegalArgumentException("Type de relation inconnu = [" + type + ']');
		}
		return relationship;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
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
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
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

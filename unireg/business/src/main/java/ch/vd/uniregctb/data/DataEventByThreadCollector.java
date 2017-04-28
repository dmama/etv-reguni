package ch.vd.uniregctb.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.xml.event.data.v1.BatimentChangeEvent;
import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseLoadEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseTruncateEvent;
import ch.vd.unireg.xml.event.data.v1.DroitAccesChangeEvent;
import ch.vd.unireg.xml.event.data.v1.ImmeubleChangeEvent;
import ch.vd.unireg.xml.event.data.v1.IndividuChangeEvent;
import ch.vd.unireg.xml.event.data.v1.OrganisationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.RelationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.Relationship;
import ch.vd.unireg.xml.event.data.v1.TiersChangeEvent;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class DataEventByThreadCollector implements DataEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataEventByThreadCollector.class);

	/**
	 * Container des données déjà émises dans la transaction courante
	 * (histoire ne ne pas envoyer plusieurs messages identiques dans une même transaction)
	 */
	private final ThreadLocal<AlreadySentData> alreadySent = ThreadLocal.withInitial(AlreadySentData::new);

	/**
	 * Liste par thread des événements à envoyer
	 */
	private final ThreadLocal<List<DataEvent>> collectedEvents = ThreadLocal.withInitial(LinkedList::new);

	/**
	 * Clé d'identification d'une relation entre tiers
	 */
	private static final class RelationshipKey implements Serializable {

		private static final long serialVersionUID = -8477087740350254408L;

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
		private final Set<Long> immeubleChange = new HashSet<>();
		private final Set<Long> batimentChange = new HashSet<>();
		private final Set<RelationshipKey> relationshipChange = new HashSet<>();

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

		/**
		 * @param id identifiant de l'immeuble qui a été modifié
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme immeuble modifié
		 */
		public boolean addImmeubleChange(Long id) {
			return immeubleChange.add(id);
		}

		/**
		 * @param id identifiant du bâtiment qui a été modifié
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme bâtiment modifié
		 */
		public boolean addBatimentChange(Long id) {
			return batimentChange.add(id);
		}
	}

	/**
	 * A appeler à la fin de chaque traitement, pour tout nettoyer en vue du traitement suivant
	 */
	public void cleanup() {
		alreadySent.remove();
		collectedEvents.remove();
	}

	/**
	 * @return la liste ordonnée des DataEvents collectés
	 */
	@NotNull
	public List<DataEvent> getCollectedEvents() {
		return Collections.unmodifiableList(collectedEvents.get());
	}

	/**
	 * Collecte des événements à envoyer au final
	 * @param event un nouvel événement à envoyer
	 */
	private void collect(DataEvent event) {
		collectedEvents.get().add(event);
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
	public void onDroitAccessChange(final long tiersId) {
		final OnNotificationAction action = data -> data.addDroitAccesChange(tiersId);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement sur les droits d'accès du tiers n°" + tiersId);
			}
			collect(new DroitAccesChangeEvent(tiersId));
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur les droits d'accès du tiers n°" + tiersId + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	public void onTiersChange(final long id) {
		final OnNotificationAction action = data -> data.addTiersChange(id);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement sur le tiers n°" + id);
			}
			collect(new TiersChangeEvent(id));
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur le tiers n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	public void onOrganisationChange(final long id) {
		final OnNotificationAction action = data -> data.addOrganisationChange(id);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement sur l'organisation n°" + id);
			}
			collect(new OrganisationChangeEvent(id));
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'organisation n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	public void onIndividuChange(final long id) {
		final OnNotificationAction action = data -> data.addIndividuChange(id);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement sur l'individu n°" + id);
			}
			collect(new IndividuChangeEvent(id));
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'individu n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		final RelationshipKey key = new RelationshipKey(type, sujetId, objetId);
		final OnNotificationAction action = data -> data.addRelationshipChange(key);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId);
			}
			collect(new RelationChangeEvent(getRelationshipMapping(type), sujetId, objetId));
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
	public void onImmeubleChange(long id) {
		final OnNotificationAction action = data -> data.addImmeubleChange(id);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement sur l'immeuble n°" + id);
			}
			collect(new ImmeubleChangeEvent(id));
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'immeuble n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	public void onBatimentChange(long id) {
		final OnNotificationAction action = data -> data.addBatimentChange(id);
		if (onNewNotification(action)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Emission d'un événement DB de changement sur le bâtiment n°" + id);
			}
			collect(new BatimentChangeEvent(id));
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur le bâtiment n°" + id + " (une émission est déjà prévue dans la transaction courante)");
		}
	}

	@Override
	public void onLoadDatabase() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Emission d'un événement de chargement de la database");
		}
		collect(new DatabaseLoadEvent());
	}

	@Override
	public void onTruncateDatabase() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Emission d'un événement de truncate de la database");
		}
		collect(new DatabaseTruncateEvent());
	}
}

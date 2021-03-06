package ch.vd.unireg.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StackedThreadLocal;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalException;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalSender;
import ch.vd.unireg.transaction.TransactionSynchronizationManagerInterface;
import ch.vd.unireg.transaction.TransactionSynchronizationRegistrar;
import ch.vd.unireg.transaction.TransactionSynchronizationSupplier;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.xml.event.data.v1.BatimentChangeEvent;
import ch.vd.unireg.xml.event.data.v1.CommunauteChangeEvent;
import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseLoadEvent;
import ch.vd.unireg.xml.event.data.v1.DatabaseTruncateEvent;
import ch.vd.unireg.xml.event.data.v1.DroitAccesChangeEvent;
import ch.vd.unireg.xml.event.data.v1.FiscalEventSendRequestEvent;
import ch.vd.unireg.xml.event.data.v1.ImmeubleChangeEvent;
import ch.vd.unireg.xml.event.data.v1.IndividuChangeEvent;
import ch.vd.unireg.xml.event.data.v1.OrganisationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.RelationChangeEvent;
import ch.vd.unireg.xml.event.data.v1.Relationship;
import ch.vd.unireg.xml.event.data.v1.TiersChangeEvent;

public class ConcentratingDataEventJmsSender implements InitializingBean, DisposableBean, EvenementFiscalSender, CivilDataEventListener, FiscalDataEventListener, TransactionSynchronizationSupplier {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConcentratingDataEventJmsSender.class);

	/**
	 * Les données collectées sur la transaction en cours
	 */
	private final StackedThreadLocal<TransactionCollectedData> transactionCollectedData = new StackedThreadLocal<>();

	private TransactionSynchronizationRegistrar synchronizationRegistrar;
	private DataEventSender sender;
	private boolean evenementsFiscauxActives;

	public void setSynchronizationRegistrar(TransactionSynchronizationRegistrar synchronizationRegistrar) {
		this.synchronizationRegistrar = synchronizationRegistrar;
	}

	public void setSender(DataEventSender sender) {
		this.sender = sender;
	}

	public void setEvenementsFiscauxActives(boolean evenementsFiscauxActives) {
		this.evenementsFiscauxActives = evenementsFiscauxActives;
	}

	/**
	 * Clé d'identification d'une relation entre tiers
	 */
	private static final class RelationshipKey implements Serializable {

		private static final long serialVersionUID = -5999701302295475545L;

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
	 * Retourne le type au format DataEvent de relation correspondant au rapport entre tiers passé en paramètre
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
		case HERITAGE:
			relationship = Relationship.HERITAGE;
			break;
		case LIENS_ASSOCIES_ET_SNC:
			relationship = Relationship.LIENS_ASSOCIES_ET_SNC;
			break;
		default:
			throw new IllegalArgumentException("Type de relation inconnu = [" + type + ']');
		}
		return relationship;
	}

	/**
	 * Ids des entités changées pour lesquelles une notification a déjà été envoyée dans la transaction courante
	 * (comme ces notifications ne sont de toute façon envoyées qu'à la fin de la transaction, rien de sert
	 * de l'envoyer plusieurs fois...)
	 */
	private static final class AlreadySentData {

		private final Set<Long> tiersChange = new HashSet<>();
		private final Set<Long> individuChange = new HashSet<>();
		private final Set<Long> entrepriseChange = new HashSet<>();
		private final Set<Long> droitsAccesChange = new HashSet<>();
		private final Set<Long> immeubleChange = new HashSet<>();
		private final Set<Long> batimentChange = new HashSet<>();
		private final Set<Long> communauteChange = new HashSet<>();
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
		 * @param id identifiant de l'entreprise modifiée
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme entreprise modifiée
		 */
		public boolean addEntrepriseChange(Long id) {
			return entrepriseChange.add(id);
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

		/**
		 * @param id identifiant de la communauté qui a été modifiée
		 * @return <code>true</code> si cet identifiant n'était pas encore connu comme celui d'une communauté modifiée
		 */
		public boolean addCommunauteChange(Long id) {
			return communauteChange.add(id);
		}
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
	 * Classe du container qui maintient la liste des données collectées pendant une transaction
	 * et programme l'envoi des événements "DataEvent" qui vont bien
	 */
	private final class TransactionCollectedData {

		private boolean sent = false;
		private final List<EvenementFiscal> evenementsFiscaux = new LinkedList<>();
		private final List<DataEvent> evenementsNotification = new LinkedList<>();
		private final AlreadySentData alreadySentNotifications = new AlreadySentData();

		private TransactionCollectedData(@NotNull TransactionSynchronizationManagerInterface mgr) {

			// enregistrement de la synchronisation dans le système
			mgr.registerSynchronization(new TransactionSynchronizationAdapter() {

				@Override
				public void suspend() {
					transactionCollectedData.pushState();
					super.suspend();
				}

				@Override
				public void resume() {
					super.resume();
					transactionCollectedData.popState();
				}

				@Override
				public void beforeCommit(boolean readOnly) {
					super.beforeCommit(readOnly);
					if (!readOnly) {
						// c'est un commit qui s'annonce, envoyons les événements collectés en une fois
						reallySendEvents(evenementsFiscaux, evenementsNotification);

						// notons que nous avons envoyé un truc... tout ce qui vient ensuite devra être envoyé individuellement
						sent = true;
					}
				}

				@Override
				public void afterCompletion(int status) {
					super.afterCompletion(status);

					// transaction committée ou annulée, il faut tout nettoyer...
					transactionCollectedData.reset();
				}
			});
		}

		private void addEvenementFiscal(EvenementFiscal evtFiscal) {
			if (sent) {
				// envoi direct si l'envoi groupé a déjà été lancé
				reallySendEvents(Collections.singletonList(evtFiscal), Collections.emptyList());
			}
			else {
				evenementsFiscaux.add(evtFiscal);
			}
		}

		private void collectNotification(DataEvent dataEvent) {
			if (sent) {
				// envoi direct si l'envoi groupé a déjà été lancé
				reallySendEvents(Collections.emptyList(), Collections.singletonList(dataEvent));
			}
			else {
				evenementsNotification.add(dataEvent);
			}
		}

		/**
		 * @param action action sur les données maintenues
		 * @return la valeur de retour de l'action
		 */
		private boolean onNewNotification(OnNotificationAction action) {
			return action.registerNotification(alreadySentNotifications);
		}

		public void onEntrepriseChange(long id) {
			final OnNotificationAction action = data -> data.addEntrepriseChange(id);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur l'entreprise n°" + id);
				}
				collectNotification(new OrganisationChangeEvent(id));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'entreprise n°" + id + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onTiersChange(long id) {
			final OnNotificationAction action = data -> data.addTiersChange(id);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur le tiers n°" + id);
				}
				collectNotification(new TiersChangeEvent(id));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur le tiers n°" + id + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onIndividuChange(long id) {
			final OnNotificationAction action = data -> data.addIndividuChange(id);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur l'individu n°" + id);
				}
				collectNotification(new IndividuChangeEvent(id));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'individu n°" + id + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onDroitAccessChange(long tiersId) {
			final OnNotificationAction action = data -> data.addDroitAccesChange(tiersId);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur les droits d'accès du tiers n°" + tiersId);
				}
				collectNotification(new DroitAccesChangeEvent(tiersId));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur les droits d'accès du tiers n°" + tiersId + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
			final RelationshipKey key = new RelationshipKey(type, sujetId, objetId);
			final OnNotificationAction action = data -> data.addRelationshipChange(key);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId);
				}
				collectNotification(new RelationChangeEvent(getRelationshipMapping(type), sujetId, objetId));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement de la relation de type " + type + " entre les tiers sujet " + sujetId + " et objet " + objetId + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onImmeubleChange(long id) {
			final OnNotificationAction action = data -> data.addImmeubleChange(id);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur l'immeuble n°" + id);
				}
				collectNotification(new ImmeubleChangeEvent(id));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur l'immeuble n°" + id + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onBatimentChange(long id) {
			final OnNotificationAction action = data -> data.addBatimentChange(id);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur le bâtiment n°" + id);
				}
				collectNotification(new BatimentChangeEvent(id));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur le bâtiment n°" + id + " (une émission est déjà prévue dans la transaction courante)");
			}
		}

		public void onCommunauteChange(long id) {
			final OnNotificationAction action = data -> data.addCommunauteChange(id);
			if (onNewNotification(action)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Emission d'un événement DB de changement sur la communauté n°" + id);
				}
				collectNotification(new CommunauteChangeEvent(id));
			}
			else if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Pas de nouvelle émission d'un événement DB de changement sur la communauté n°" + id + " (une émission est déjà prévue dans la transaction courante)");
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		synchronizationRegistrar.registerSynchronizationSupplier(this);
	}

	@Override
	public void destroy() throws Exception {
		synchronizationRegistrar.unregisterSynchronizationSupplier(this);
	}

	@Override
	public void registerSynchronizations(@NotNull TransactionSynchronizationManagerInterface mgr) {
		// on n'enregistre une synchronisation que si elle n'est pas déjà enregistrée
		if (transactionCollectedData.get() == null) {
			transactionCollectedData.set(new TransactionCollectedData(mgr));
		}
	}

	private void reallySendEvents(List<EvenementFiscal> evenementsFiscaux, List<DataEvent> evenementsNotification) {
		final List<DataEvent> events;
		if (!evenementsFiscaux.isEmpty()) {
			final DataEvent evtFiscalDataEvent = new FiscalEventSendRequestEvent(evenementsFiscaux.stream()
					                                                                     .map(EvenementFiscal::getId)
					                                                                     .collect(Collectors.toList()));
			events = Stream.concat(evenementsNotification.stream(), Stream.of(evtFiscalDataEvent))
					.collect(Collectors.toList());
		}
		else {
			events = evenementsNotification;
		}

		if (!events.isEmpty()) {
			try {
				sender.sendDataEvent(events);
			}
			catch (Exception e) {
				LOGGER.error("Impossible d'envoyer les 'dataEvents' : [" + CollectionsUtils.toString(events, StringRenderer.DEFAULT, ", ") + "]", e);
			}
		}
	}

	private void delegate(Consumer<TransactionCollectedData> action) {
		action.accept(transactionCollectedData.get());
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		if (!evenementsFiscauxActives) {
			LOGGER.info(String.format("Evénements fiscaux désactivés : l'événement fiscal %d n'est pas envoyé.", evenement.getId()));
			return;
		}
		delegate(data -> data.addEvenementFiscal(evenement));
	}

	@Override
	public void onEntrepriseChange(long id) {
		delegate(data -> data.onEntrepriseChange(id));
	}

	@Override
	public void onTiersChange(long id) {
		delegate(data -> data.onTiersChange(id));
	}

	@Override
	public void onIndividuChange(long id) {
		delegate(data -> data.onIndividuChange(id));
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		delegate(data -> data.onDroitAccessChange(tiersId));
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		delegate(data -> data.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		delegate(data -> data.onImmeubleChange(immeubleId));
	}

	@Override
	public void onBatimentChange(long batimentId) {
		delegate(data -> data.onBatimentChange(batimentId));
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		delegate(data -> data.onCommunauteChange(communauteId));
	}

	@Override
	public void onTruncateDatabase() {
		// pas de concentration de cet événement (qui n'est utilisé qu'en test) -> on l'envoie directement
		reallySendEvents(Collections.emptyList(), Collections.singletonList(new DatabaseTruncateEvent()));
	}

	@Override
	public void onLoadDatabase() {
		// pas de concentration de cet événement (qui n'est utilisé qu'en test) -> on l'envoie directement
		reallySendEvents(Collections.emptyList(), Collections.singletonList(new DatabaseLoadEvent()));
	}
}

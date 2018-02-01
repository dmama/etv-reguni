package ch.vd.unireg.efacture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.EtatDemande;
import ch.vd.unireg.interfaces.efacture.data.EtatDestinataire;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.type.TypeDocument;

/**
 * Toute modification passe par des entités {@link Action} pour simuler le côté asynchrone de certaines communications avec le véritable service,
 * ces actions sont ensuite activées lors de l'appel à {@link #commit()} (ou purgées lors de l'appel à {@link #rollback()}
 */
public abstract class MockEFactureService implements EFactureService {

	/**
	 * Méthode d'initialisation des données mockées
 	 */
	public abstract void init();

	/**
	 * Historique des données e-facture du destinataire (= contribuable)
	 */
	private final Map<Long, DestinataireAvecHisto> data = new HashMap<>();

	/**
	 * Données des demandes (également présentes dans l'historique, mais indexées par ID de demande)
	 */
	private final Map<String, DemandeAvecHisto> demandes = new HashMap<>();

	/**
	 * Rempli par les demandes de modifications (asynchrones) et vidé lors du {@link #commit()} ou {@link #rollback()}
	 */
	private final List<Action> actions = new ArrayList<>();

	private interface Action {
		void execute();
	}

	protected void addDestinataire(long ctbId) {
		final DestinataireAvecHisto histo = new DestinataireAvecHisto(ctbId, TypeEtatDestinataire.NON_INSCRIT);
		data.put(ctbId, histo);
	}

	protected void addEtatDestinataire(long ctbId, Date date, String descriptionRaison, Integer codeRaison, TypeEtatDestinataire etat, String email, BigInteger noAdherent) {
		final DestinataireAvecHisto histo = data.get(ctbId);
		final EtatDestinataire etatDest = new EtatDestinataire(null, date, descriptionRaison, codeRaison, etat, email, noAdherent);
		histo.getHistoriquesEtats().add(etatDest);
	}

	protected DemandeAvecHisto addDemandeInscription(String id, long ctbId, String email, RegDate dateDemande, Demande.Action action, String noAvs, TypeEtatDemande etatDemande, BigInteger noAdherent) {
		final DemandeAvecHisto demande = new DemandeAvecHisto(id, ctbId, email, dateDemande, action, noAvs, noAdherent);
		demande.getHistoriqueEtats().add(EtatDemande.newEtatDemandeFactice(etatDemande));

		final DestinataireAvecHisto dest = data.get(ctbId);
		dest.getHistoriqueDemandes().add(demande);
		demandes.put(demande.getIdDemande(), demande);
		return demande;
	}

	protected void addEtatDemande(DemandeAvecHisto demande, Date date, Integer codeRaison, String descriptionRaison, TypeEtatDemande etat) {
		final EtatDemande etatDemande = new EtatDemande(null, date, codeRaison, descriptionRaison, etat);
		demande.getHistoriqueEtats().add(etatDemande);
	}

	private final class MettreEnAttenteInscription implements Action {
		private final String idDemande;
		private final TypeAttenteDemande typeAttenteDemande;
		private final String description;

		private MettreEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteDemande, String description) {
			this.idDemande = idDemande;
			this.typeAttenteDemande = typeAttenteDemande;
			this.description = description;
		}

		@Override
		public void execute() {
			final DemandeAvecHisto demande = demandes.get(idDemande);
			final EtatDemande etat = new EtatDemande(description, DateHelper.getCurrentDate(),
			                                         typeAttenteDemande.getCode(), typeAttenteDemande.getDescription(),
			                                         TypeEtatDemande.valueOf(RegistrationRequestStatus.VALIDATION_EN_COURS, typeAttenteDemande));
			demande.getHistoriqueEtats().add(etat);
		}
	}

	@Override
	public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		actions.add(new MettreEnAttenteInscription(idDemande, typeAttenteEFacture, description));
		return StringUtils.EMPTY;
	}

	@Override
	public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException {
		return StringUtils.EMPTY;
	}

	@Override
	public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
		return data.get(ctbId);
	}

	private final class SuspendreContribuable implements Action {
		private final long ctbId;
		private final String description;

		private SuspendreContribuable(long ctbId, String description) {
			this.ctbId = ctbId;
			this.description = description;
		}

		@Override
		public void execute() {
			final DestinataireAvecHisto dest = data.get(ctbId);
			final EtatDestinataire dernierEtat = dest.getDernierEtat();
			final TypeEtatDestinataire nouveauType = dernierEtat.getType().avecSuspension();
			if (nouveauType != dernierEtat.getType()) {
				final EtatDestinataire etat = new EtatDestinataire(description, DateHelper.getCurrentDate(), null, null, nouveauType, dernierEtat.getEmail(), dernierEtat.getNoAdherent());
				dest.getHistoriquesEtats().add(etat);
			}
		}
	}

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		actions.add(new SuspendreContribuable(ctbId, description));
		return StringUtils.EMPTY;
	}

	private final class ActiverContribuable implements Action {
		private final long ctbId;
		private final String description;

		private ActiverContribuable(long ctbId, String description) {
			this.ctbId = ctbId;
			this.description = description;
		}

		@Override
		public void execute() {
			final DestinataireAvecHisto dest = data.get(ctbId);
			final EtatDestinataire dernierEtat = dest.getDernierEtat();
			final TypeEtatDestinataire nouveauType = dernierEtat.getType().avecActivation();
			if (nouveauType != dernierEtat.getType()) {
				final EtatDestinataire etat = new EtatDestinataire(null, DateHelper.getCurrentDate(), description, null, nouveauType, dernierEtat.getEmail(), dernierEtat.getNoAdherent());
				dest.getHistoriquesEtats().add(etat);
			}
		}
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		actions.add(new ActiverContribuable(ctbId, description));
		return StringUtils.EMPTY;
	}

	private final class AccepterDemande implements Action {
		private final String idDemande;
		private final String description;

		private AccepterDemande(String idDemande, String description) {
			this.idDemande = idDemande;
			this.description = description;
		}

		@Override
		public void execute() {
			// on accepte la demande et on passe le contribuable en "inscrit"
			final DemandeAvecHisto demande = demandes.get(idDemande);
			final EtatDemande etatDemande = new EtatDemande(null, DateHelper.getCurrentDate(), null, description, TypeEtatDemande.VALIDEE);
			demande.getHistoriqueEtats().add(etatDemande);

			final long ctbId = demande.getCtbId();
			final DestinataireAvecHisto dest = data.get(ctbId);
			final EtatDestinataire dernierEtat = dest.getDernierEtat();
			final TypeEtatDestinataire nouveauType = dernierEtat.getType().avecInscription();
			if (nouveauType != dernierEtat.getType()) {
				final EtatDestinataire etatDestinataire = new EtatDestinataire(null, DateHelper.getCurrentDate(), description, null, nouveauType, demande.getEmail(), demande.getNoAdherent());
				dest.getHistoriquesEtats().add(etatDestinataire);
			}
		}
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		actions.add(new AccepterDemande(idDemande, description));
		return StringUtils.EMPTY;
	}

	private final class RefuserDemande implements Action {
		private final String idDemande;
		private final String description;

		private RefuserDemande(String idDemande, String description) {
			this.idDemande = idDemande;
			this.description = description;
		}

		@Override
		public void execute() {
			final DemandeAvecHisto demande = demandes.get(idDemande);
			final EtatDemande etatDemande = new EtatDemande(null, DateHelper.getCurrentDate(), null, description, TypeEtatDemande.REFUSEE);
			demande.getHistoriqueEtats().add(etatDemande);
		}
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		actions.add(new RefuserDemande(idDemande, description));
		return StringUtils.EMPTY;
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException {
		// il faut trouver la demande en cours de validation et l'accepter
		final DestinataireAvecHisto dest = data.get(noCtb);
		final List<DemandeAvecHisto> histoDemandes = dest.getHistoriqueDemandes();
		DemandeAvecHisto enCours = null;
		for (DemandeAvecHisto candidate : CollectionsUtils.revertedOrder(histoDemandes)) {
			if (candidate.getDernierEtat().getType() == TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE) {
				enCours = candidate;
				break;
			}
		}

		if (enCours == null) {
			return ResultatQuittancement.aucuneDemandeEnAttenteDeSignature();
		}
		final String bid = accepterDemande(enCours.getIdDemande(), false, "Quittancement 'douchette'");
		return ResultatQuittancement.enCours(bid);
	}

	private final class ModifierEmail implements Action {
		private final long noCtb;
		private final String newEmail;
		private final String description;

		private ModifierEmail(long noCtb, String newEmail, String description) {
			this.noCtb = noCtb;
			this.newEmail = newEmail;
			this.description = description;
		}

		@Override
		public void execute() {
			final DestinataireAvecHisto dest = data.get(noCtb);
			final EtatDestinataire dernierEtat = dest.getDernierEtat();
			final EtatDestinataire etatDestinataire = new EtatDestinataire(null, DateHelper.getCurrentDate(), description, null, dernierEtat.getType(), newEmail, dernierEtat.getNoAdherent());
			dest.getHistoriquesEtats().add(etatDestinataire);
		}
	}

	@Override
	public String modifierEmailContribuable(long noCtb, @Nullable String newEmail, boolean retourAttendu, String description) throws EvenementEfactureException {
		actions.add(new ModifierEmail(noCtb, newEmail, description));
		return StringUtils.EMPTY;
	}

	private final class Desinscrire implements Action {
		private final long noCtb;
		private final String idDemandeInscriptionRemplacante;
		private final String description;

		private Desinscrire(long noCtb, String idDemandeInscriptionRemplacante, String description) {
			this.noCtb = noCtb;
			this.idDemandeInscriptionRemplacante = idDemandeInscriptionRemplacante;
			this.description = description;
		}

		@Override
		public void execute() {
			final DestinataireAvecHisto dest = data.get(noCtb);
			if (dest.isInscrit()) {
				final EtatDestinataire etatDestinataire = new EtatDestinataire(null, DateHelper.getCurrentDate(), description, null, dest.getDernierEtat().getType().avecDesinscription(), null, null);
				dest.getHistoriquesEtats().add(etatDestinataire);
			}
			final List<DemandeAvecHisto> histoDemandes = dest.getHistoriqueDemandes();
			for (DemandeAvecHisto demande : histoDemandes) {
				if (idDemandeInscriptionRemplacante.equals(demande.getIdDemande())) {
					break;
				}
				if (demande.getDernierEtat().getType().isEnCours()) {
					final EtatDemande etatDemande = new EtatDemande(null, DateHelper.getCurrentDate(), null, description, TypeEtatDemande.ANNULEE);
					demande.getHistoriqueEtats().add(etatDemande);
				}
			}
		}
	}

	@Override
	public void demanderDesinscriptionContribuable(long noCtb, String idNouvelleDemande, String description) throws EvenementEfactureException {
		actions.add(new Desinscrire(noCtb, idNouvelleDemande, description));
	}

	/**
	 * Exécute puis purge les actions enregistrées
	 */
	public void commit() {
		for (Action action : actions) {
			action.execute();
		}
		actions.clear();
	}

	/**
	 * Purge les actions enregistrées sans les exéctuter
	 */
	public void rollback() {
		actions.clear();
	}
}

package ch.vd.uniregctb.efacture;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

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
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

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

	protected void addDestinataire(long ctbId) {
		final DestinataireAvecHisto histo = new DestinataireAvecHisto(ctbId, TypeEtatDestinataire.DESINSCRIT);
		data.put(ctbId, histo);
	}

	protected void addEtatDestinataire(long ctbId, Date date, String descriptionRaison, Integer codeRaison, TypeEtatDestinataire etat) {
		final DestinataireAvecHisto histo = data.get(ctbId);
		final EtatDestinataire etatDest = new EtatDestinataire(null, date, descriptionRaison, codeRaison, etat);
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

	@Override
	public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		final DemandeAvecHisto demande = demandes.get(idDemande);
		final EtatDemande etat = new EtatDemande(description, DateHelper.getCurrentDate(),
		                                         typeAttenteEFacture.getCode(), typeAttenteEFacture.getDescription(),
		                                         TypeEtatDemande.valueOf(RegistrationRequestStatus.VALIDATION_EN_COURS, typeAttenteEFacture));
		demande.getHistoriqueEtats().add(etat);
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

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		final DestinataireAvecHisto dest = data.get(ctbId);
		final EtatDestinataire dernierEtat = dest.getDernierEtat();
		final TypeEtatDestinataire nouveauType;
		switch (dernierEtat.getType()) {
			case DESINSCRIT:
			case DESINSCRIT_SUSPENDU:
				nouveauType = TypeEtatDestinataire.DESINSCRIT_SUSPENDU;
				break;
			case INSCRIT:
			case INSCRIT_SUSPENDU:
				nouveauType = TypeEtatDestinataire.INSCRIT_SUSPENDU;
				break;
			default:
				throw new IllegalStateException("Invalid value: " + dernierEtat.getType());
		}

		final EtatDestinataire etat = new EtatDestinataire(description, DateHelper.getCurrentDate(), null, null, nouveauType);
		dest.getHistoriquesEtats().add(etat);
		return StringUtils.EMPTY;
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		final DestinataireAvecHisto dest = data.get(ctbId);
		final EtatDestinataire dernierEtat = dest.getDernierEtat();
		final TypeEtatDestinataire nouveauType;
		switch (dernierEtat.getType()) {
			case DESINSCRIT:
			case DESINSCRIT_SUSPENDU:
				nouveauType = TypeEtatDestinataire.DESINSCRIT;
				break;
			case INSCRIT:
			case INSCRIT_SUSPENDU:
				nouveauType = TypeEtatDestinataire.INSCRIT;
				break;
			default:
				throw new IllegalStateException("Invalid value: " + dernierEtat.getType());
		}

		final EtatDestinataire etat = new EtatDestinataire(description, DateHelper.getCurrentDate(), null, null, nouveauType);
		dest.getHistoriquesEtats().add(etat);
		return StringUtils.EMPTY;
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		// on accepte la demande et on passe le contribuable en "inscrit"
		final DemandeAvecHisto demande = demandes.get(idDemande);
		final EtatDemande etatDemande = new EtatDemande(description, DateHelper.getCurrentDate(), null, null, TypeEtatDemande.VALIDEE);
		demande.getHistoriqueEtats().add(etatDemande);

		final long ctbId = demande.getCtbId();
		final DestinataireAvecHisto dest = data.get(ctbId);
		final EtatDestinataire dernierEtat = dest.getDernierEtat();
		final TypeEtatDestinataire nouveauType;
		switch (dernierEtat.getType()) {
			case DESINSCRIT:
			case INSCRIT:
				nouveauType = TypeEtatDestinataire.INSCRIT;
				break;
			case DESINSCRIT_SUSPENDU:
			case INSCRIT_SUSPENDU:
				nouveauType = TypeEtatDestinataire.INSCRIT_SUSPENDU;
				break;
			default:
				throw new IllegalStateException("Invalid value: " + dernierEtat.getType());
		}
		final EtatDestinataire etatDestinataire = new EtatDestinataire(description, DateHelper.getCurrentDate(), null, null, nouveauType);
		dest.getHistoriquesEtats().add(etatDestinataire);
		return StringUtils.EMPTY;
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		final DemandeAvecHisto demande = demandes.get(idDemande);
		final EtatDemande etatDemande = new EtatDemande(description, DateHelper.getCurrentDate(), null, null, TypeEtatDemande.REFUSEE);
		demande.getHistoriqueEtats().add(etatDemande);
		return StringUtils.EMPTY;
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException {
		// il faut trouver la demande en cours de validation et l'accepter
		final DestinataireAvecHisto dest = data.get(noCtb);
		final List<DemandeAvecHisto> histoDemandes = dest.getHistoriqueDemandes();
		DemandeAvecHisto enCours = null;
		for (DemandeAvecHisto candidate : histoDemandes) {
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
}

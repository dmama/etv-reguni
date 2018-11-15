package ch.vd.unireg.mouvement.view;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.mouvement.EtatMouvementDossier;
import ch.vd.unireg.type.Localisation;
import ch.vd.unireg.type.TypeMouvement;

/**
 * Critères de recherche pour les mouvements de masse
 */
public abstract class MouvementMasseCriteriaView {

	/**
	 * Numéro du contribuable
	 */
	private Long noCtb;

	/**
	 * Numéro du contribuable sous forme formattée
	 */
	private String noCtbFormatte;

	/**
	 * Limite minimale (la plus lointaine dans le passé) des dates de mouvements
	 */
	private Date dateMouvementMin;

	/**
	 * Limite maximale (la plus proche dans le passé) des dates de mouvements
	 */
	private Date dateMouvementMax;

	/**
	 * Type de mouvement (envoi / réception)
	 */
	private TypeMouvement typeMouvement;

	/**
	 * Collectivité administrative réceptrice du mouvement (destinataire d'un envoi)
	 */
	private Integer noCollAdmDestinataire;
	private String collAdmDestinataire;

	/**
	 * Numéro d'individu de l'opérateur destinataire d'un envoi
	 */
	private String visaDestinataire;
	private String nomDestinataire;

	/**
	 * Numéro d'individu de l'opérateur réceptioniste d'une réception "personne"
	 */
	private String visaRecepteur;
	private String nomRecepteur;

	/**
	 * Type de réception
	 */
	private Localisation localisationReception;

	/**
	 * Si oui ou non les mouvements annulés doivent être inclus
	 */
	private boolean mouvementsAnnulesInclus;

	/**
	 * Si oui ou non il faut se limiter au dernier mouvement traité de chaque dossier
	 */
	private boolean seulementDernierMouvementDossiers;

	public Long getNoCtb() {
		return noCtb;
	}

	public void setNoCtb(Long noCtb) {
		this.noCtb = noCtb;
	}

	public Date getDateMouvementMin() {
		return dateMouvementMin;
	}

	public void setDateMouvementMin(Date dateMouvementMin) {
		this.dateMouvementMin = dateMouvementMin;
	}

	public Date getDateMouvementMax() {
		return dateMouvementMax;
	}

	public void setDateMouvementMax(Date dateMouvementMax) {
		this.dateMouvementMax = dateMouvementMax;
	}

	public TypeMouvement getTypeMouvement() {
		return typeMouvement;
	}

	public void setTypeMouvement(TypeMouvement typeMouvement) {
		this.typeMouvement = typeMouvement;
	}

	public Integer getNoCollAdmDestinataire() {
		return noCollAdmDestinataire;
	}

	public void setNoCollAdmDestinataire(Integer noCollAdmDestinataire) {
		this.noCollAdmDestinataire = noCollAdmDestinataire;
	}

	public String getCollAdmDestinataire() {
		return collAdmDestinataire;
	}

	public void setCollAdmDestinataire(String collAdmDestinataire) {
		this.collAdmDestinataire = collAdmDestinataire;
	}

	public String getVisaDestinataire() {
		return visaDestinataire;
	}

	public void setVisaDestinataire(String visaDestinataire) {
		this.visaDestinataire = visaDestinataire;
	}

	public String getNomDestinataire() {
		return nomDestinataire;
	}

	public void setNomDestinataire(String nomDestinataire) {
		this.nomDestinataire = nomDestinataire;
	}

	public String getVisaRecepteur() {
		return visaRecepteur;
	}

	public void setVisaRecepteur(String visaRecepteur) {
		this.visaRecepteur = visaRecepteur;
	}

	public String getNomRecepteur() {
		return nomRecepteur;
	}

	public void setNomRecepteur(String nomRecepteur) {
		this.nomRecepteur = nomRecepteur;
	}

	public Localisation getLocalisationReception() {
		return localisationReception;
	}

	public void setLocalisationReception(Localisation localisationReception) {
		this.localisationReception = localisationReception;
	}

	public boolean isMouvementsAnnulesInclus() {
		return mouvementsAnnulesInclus;
	}

	public void setMouvementsAnnulesInclus(boolean mouvementsAnnulesInclus) {
		this.mouvementsAnnulesInclus = mouvementsAnnulesInclus;
	}

	public boolean isSeulementDernierMouvementDossiers() {
		return seulementDernierMouvementDossiers;
	}

	public void setSeulementDernierMouvementDossiers(boolean seulementDernierMouvementDossiers) {
		this.seulementDernierMouvementDossiers = seulementDernierMouvementDossiers;
	}

	public String getNoCtbFormatte() {
		return noCtbFormatte;
	}

	public void setNoCtbFormatte(String noCtbFormatte) {
		setNoCtb(null);
		if (!StringUtils.isBlank(noCtbFormatte)) {
			try {
				final Long no = Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(noCtbFormatte));
				setNoCtb(no);
			}
			catch (NumberFormatException e) {
				// pas grave, le numéro est juste invalide...
			}
		}
		this.noCtbFormatte = noCtbFormatte;
	}

	/**
	 * Nettoyage des critères de recherche et des résultats
	 */
	public void init() {
		this.collAdmDestinataire = null;
		this.dateMouvementMax = null;
		this.dateMouvementMin = null;
		this.visaDestinataire = null;
		this.nomDestinataire = null;
		this.visaRecepteur = null;
		this.nomRecepteur = null;
		this.localisationReception = null;
		this.mouvementsAnnulesInclus = false;
		this.noCollAdmDestinataire = null;
		this.noCtb = null;
		this.noCtbFormatte = null;
		this.typeMouvement = null;
	}

	/**
	 * Retourne une collection contenant le ou les états de mouvements de dossiers à rechercher, ou <code>null</code>
	 * si aucun critère ne doit se faire sur l'état des mouvements
	 * @return les états concernés
	 */
	public abstract Collection<EtatMouvementDossier> getEtatsRecherches();
}

package ch.vd.unireg.mouvement;

import java.util.Collection;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.type.Localisation;
import ch.vd.unireg.type.TypeMouvement;

public class MouvementDossierCriteria {

	/**
	 * Type des mouvements à rechercher
	 */
	private TypeMouvement typeMouvement;

	/**
	 * Etats possibles des mouvements à rechercher
	 */
	private Collection<EtatMouvementDossier> etatsMouvement;

	/**
	 * Numéro de dossier des mouvements à rechercher
	 */
	private Long noCtb;

	/**
	 * Période (potentiellement ouverte) des dates de traitement des mouvements à rechercher
	 */
	private DateRange rangeDateMouvement;

	/**
	 * ID technique de la collectivité administrative initiatrice du mouvement (pour filtrage OID autentifié)
	 */
	private Long idCollAdministrativeInitiatrice;

	/**
	 * ID technique de la collectivité administrative destinataire des mouvements d'envoi
	 */
	private Long idCollAdministrativeDestinataire;

	/**
	 * Visa du collaborateur destinataire des mouvements d'envoi
	 */
	private String visaDestinataire;

	/**
	 * Visa du collaborateur récepteur des mouvements
	 */
	private String visaRecepteur;

	/**
	 * Type de mouvement de réception
	 */
	private Localisation localisation;

	/**
	 * Indique si on doit filtrer sur les derniers mouvements de chaque dossier (pour avoir l'emplacement actuel) (<code>true</code>) ou si tout l'historique doit être recherché (<code>false</code>)
	 */
	private boolean seulementDerniersMouvements;

	/**
	 * Indique si oui ou non les mouvements annulés (au sens du flag, pas de l'état) doivent être inclus dans le résultat de la recherche
	 */
	private boolean isInclureMouvementsAnnules;

	public TypeMouvement getTypeMouvement() {
		return typeMouvement;
	}

	public void setTypeMouvement(TypeMouvement typeMouvement) {
		this.typeMouvement = typeMouvement;
	}

	public Collection<EtatMouvementDossier> getEtatsMouvement() {
		return etatsMouvement;
	}

	public void setEtatsMouvement(Collection<EtatMouvementDossier> etatsMouvement) {
		this.etatsMouvement = etatsMouvement;
	}

	public Long getNoCtb() {
		return noCtb;
	}

	public void setNoCtb(Long noCtb) {
		this.noCtb = noCtb;
	}

	public DateRange getRangeDateMouvement() {
		return rangeDateMouvement;
	}

	public void setRangeDateMouvement(DateRange rangeDateMouvement) {
		this.rangeDateMouvement = rangeDateMouvement;
	}

	public Long getIdCollAdministrativeDestinataire() {
		return idCollAdministrativeDestinataire;
	}

	public void setIdCollAdministrativeDestinataire(Long idCollAdministrativeDestinataire) {
		this.idCollAdministrativeDestinataire = idCollAdministrativeDestinataire;
	}

	public String getVisaDestinataire() {
		return visaDestinataire;
	}

	public void setVisaDestinataire(String visaDestinataire) {
		this.visaDestinataire = visaDestinataire;
	}

	public String getVisaRecepteur() {
		return visaRecepteur;
	}

	public void setVisaRecepteur(String visaRecepteur) {
		this.visaRecepteur = visaRecepteur;
	}

	public Localisation getLocalisation() {
		return localisation;
	}

	public void setLocalisation(Localisation localisation) {
		this.localisation = localisation;
	}

	public boolean isInclureMouvementsAnnules() {
		return isInclureMouvementsAnnules;
	}

	public void setInclureMouvementsAnnules(boolean inclureMouvementsAnnules) {
		isInclureMouvementsAnnules = inclureMouvementsAnnules;
	}

	public boolean isSeulementDerniersMouvements() {
		return seulementDerniersMouvements;
	}

	public void setSeulementDerniersMouvements(boolean seulementDerniersMouvements) {
		this.seulementDerniersMouvements = seulementDerniersMouvements;
	}

	public Long getIdCollAdministrativeInitiatrice() {
		return idCollAdministrativeInitiatrice;
	}

	public void setIdCollAdministrativeInitiatrice(Long idCollAdministrativeInitiatrice) {
		this.idCollAdministrativeInitiatrice = idCollAdministrativeInitiatrice;
	}
}

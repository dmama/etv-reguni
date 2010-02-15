package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.validation.ValidationResults;

@Entity
@DiscriminatorValue("AdresseSuisse")
public class AdresseSuisse extends AdresseSupplementaire {

	private static final long serialVersionUID = 2539958821652480740L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Numéro de la rue du répertoire des rues fourni par DCL Data Care (entreprise de la Poste suisse) pour compléter l'offre NPA.
	 * Pour plus de détail, consulter le fichier "Répertoire des rues. Description de l'offre et structure des données" disponible sur le site http://www.match.ch
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi94Vx9Edygsbnw9h5bVw"
	 */
	private Integer numeroRue;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Le numéro d’ordre Poste constitue la partie-clé du NPA; elle est unique et ne peut être modifiée.
	 * Chaque nouveau NPA reçoit un nouveau ONRP. Le ONRP reste inchangé, même si le NPA lui-même
	 * change.
	 * Lorsqu’un NPA est mis hors service, son ONRP n’est plus utilisé.
	 * Si le NPA devait être remis en service (ce qui est très rare), ce serait avec le ONRP d’origine.
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi941x9Edygsbnw9h5bVw"
	 */
	private Integer numeroOrdrePoste;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroRue
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi94Vx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_RUE")
	public Integer getNumeroRue() {
		// begin-user-code
		return numeroRue;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroRue the numeroRue to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi94Vx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroRue(Integer theNumeroRue) {
		// begin-user-code
		numeroRue = theNumeroRue;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroOrdrePoste
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi941x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_ORDRE_POSTE")
	public Integer getNumeroOrdrePoste() {
		// begin-user-code
		return numeroOrdrePoste;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroOrdrePoste the numeroOrdrePoste to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi941x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroOrdrePoste(Integer theNumeroOrdrePoste) {
		// begin-user-code
		numeroOrdrePoste = theNumeroOrdrePoste;
		// end-user-code
	}

	public ValidationResults validate() {
		ValidationResults results = new ValidationResults();
		if ((numeroRue == null || numeroRue == 0) && (numeroOrdrePoste == null || numeroOrdrePoste == 0)) {
			results.addError("Le numéro de rue ou le numéro d'ordre poste doit être renseigné sur une adresse suisse [" + this + "]");
		}
		return results;
	}
}

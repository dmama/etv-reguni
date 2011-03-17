/**
 *
 */
package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TexteCasePostale;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * Adresse libre non issue d'une adresse de l'individu ou de l'adresse courrier du représentant légal
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9wFx9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9wFx9Edygsbnw9h5bVw"
 */
@Entity
public abstract class AdresseSupplementaire extends AdresseTiers {
	/**
	 *
	 */
	private static final long serialVersionUID = -9160275750639533984L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Lgne libre additionnelle pour les données d?adresse supplémentaires qui ne trouvent pas leur place dans les autres champs de l?adresse (p. ex. pour la mention c/o, etc.).
	 * Longueur maximum selon eCH-0010 : 60
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9xVx9Edygsbnw9h5bVw"
	 */
	private String complement;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the complement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9xVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "COMPLEMENT", length = LengthConstants.ADRESSE_NOM)
	public String getComplement() {
		// begin-user-code
		return complement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theComplement the complement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9xVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setComplement(String theComplement) {
		// begin-user-code
		complement = theComplement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Désignation de la rue en texte libre.
	 * Valable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9x1x9Edygsbnw9h5bVw"
	 */
	private String rue;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the rue
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9x1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "RUE", length = LengthConstants.ADRESSE_NOM)
	public String getRue() {
		// begin-user-code
		return rue;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theRue the rue to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9x1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setRue(String theRue) {
		// begin-user-code
		rue = theRue;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Numéro de la maison dans l'adresse postale, y compris des indications additionnelles.
	 * Longueur maximum selon eCH-0010 : 12
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9yVx9Edygsbnw9h5bVw"
	 */
	private String numeroMaison;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroMaison
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9yVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_MAISON", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroMaison() {
		// begin-user-code
		return numeroMaison;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroMaison the numeroMaison to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9yVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroMaison(String theNumeroMaison) {
		// begin-user-code
		numeroMaison = theNumeroMaison;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Numéro de l'appartement. Ce numéro est éventuellement nécessaire dans le cadre de grands ensembles.
	 * Longueur maximum selon eCH-0011 : 10
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9y1x9Edygsbnw9h5bVw"
	 */
	private String numeroAppartement;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroAppartement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9y1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_APPARTEMENT", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroAppartement() {
		// begin-user-code
		return numeroAppartement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroAppartement the numeroAppartement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9y1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroAppartement(String theNumeroAppartement) {
		// begin-user-code
		numeroAppartement = theNumeroAppartement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Texte de la case postale dans la langue voulue.
	 * Dans la plupart des cas, le texte "Case postale" ou "Boîte postale" suffit.
	 * Longueur maximum selon eCH-0010 : 15
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9zVx9Edygsbnw9h5bVw"
	 */
	private TexteCasePostale texteCasePostale = TexteCasePostale.CASE_POSTALE;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the texteCasePostale
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9zVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "TEXTE_CASE_POSTALE", length = LengthConstants.ADRESSE_TYPESUPPLEM)
	@Type(type = "ch.vd.uniregctb.hibernate.TexteCasePostaleUserType")
	public TexteCasePostale getTexteCasePostale() {
		// begin-user-code
		return texteCasePostale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTexteCasePostale the texteCasePostale to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9zVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setTexteCasePostale(TexteCasePostale theTexteCasePostale) {
		// begin-user-code
		texteCasePostale = theTexteCasePostale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Numéro de la case postale
	 * Valeurs admises selon eCH-0010 : 0-9999
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9z1x9Edygsbnw9h5bVw"
	 */
	private Integer numeroCasePostale;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroCasePostale
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9z1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_CASE_POSTALE")
	public Integer getNumeroCasePostale() {
		// begin-user-code
		return numeroCasePostale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroCasePostale the numeroCasePostale to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi9z1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroCasePostale(Integer theNumeroCasePostale) {
		// begin-user-code
		numeroCasePostale = theNumeroCasePostale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3BIaoC3-Ed2H4bonmeBdag"
	 */
	private boolean permanente = false;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the permanente
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3BIaoC3-Ed2H4bonmeBdag?GETTER"
	 */
	@Column(name = "PERMANENTE")
	public boolean isPermanente() {
		// begin-user-code
		return permanente;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param thePermanente the permanente to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3BIaoC3-Ed2H4bonmeBdag?SETTER"
	 */
	public void setPermanente(boolean thePermanente) {
		// begin-user-code
		permanente = thePermanente;
		// end-user-code
	}
}
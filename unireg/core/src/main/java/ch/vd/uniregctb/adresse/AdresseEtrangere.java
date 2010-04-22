package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue("AdresseEtrangere")
public class AdresseEtrangere extends AdresseSupplementaire {

	private static final long serialVersionUID = 1259894971076020765L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Données complémentaires quant au lieu, comme par exemple la région, la province, l'état fédéral ou le quartier.
	 * Longueur selon eCH-0010 : 40
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi901x9Edygsbnw9h5bVw"
	 */
	private String complementLocalite;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the complementLocalite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi901x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "COMPLEMENT_LOCALITE", length = LengthConstants.ADRESSE_NOM)
	public String getComplementLocalite() {
		// begin-user-code
		return complementLocalite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theComplementLocalite the complementLocalite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi901x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setComplementLocalite(String theComplementLocalite) {
		// begin-user-code
		complementLocalite = theComplementLocalite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Lieu de l'adresse.
	 * Longueur selon eCH-0010 : 40
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi91Vx9Edygsbnw9h5bVw"
	 */
	private String numeroPostalLocalite;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroPostalLocalite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi91Vx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_POSTAL_LOCALITE", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroPostalLocalite() {
		// begin-user-code
		return numeroPostalLocalite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroPostalLocalite the numeroPostalLocalite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi91Vx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroPostalLocalite(String theNumeroPostalLocalite) {
		// begin-user-code
		numeroPostalLocalite = theNumeroPostalLocalite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Abréviation ISO 3166-1 du pays dans lequel se trouve le lieu faisant partie de l'adresse postale.
	 * Longueur selon eCH-0010 : 2
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi92Vx9Edygsbnw9h5bVw"
	 */
	private Integer numeroOfsPays;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroOfsPays
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi92Vx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_OFS_PAYS")
	public Integer getNumeroOfsPays() {
		// begin-user-code
		return numeroOfsPays;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroOfsPays the numeroOfsPays to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi92Vx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroOfsPays(Integer theNumeroOfsPays) {
		// begin-user-code
		numeroOfsPays = theNumeroOfsPays;
		// end-user-code
	}

	public ValidationResults validate() {
		ValidationResults results = new ValidationResults();
		if ((numeroOfsPays == null || numeroOfsPays == 0)) {
			results.addError("Le numéro Ofs du pays doit être renseigné sur une adresse étrangère [" + this + "]");
		}
		return results;
	}
}

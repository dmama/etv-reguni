package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.FormeJuridique;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * Organisation inconnue du registre des personnes morales de l'ACI.
 * Comprend également certains services publics : office du tuteur général, offices des poursuites, administrations fiscales...
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8d1x9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8d1x9Edygsbnw9h5bVw"
 */
@Entity
@DiscriminatorValue("AutreCommunaute")
public class AutreCommunaute extends ContribuableImpositionPersonnesMorales {

	// Numéros générés pour AutreCommunauté et CollectiviteAdministrative
	public static final int CAAC_GEN_FIRST_ID = 2000000;
	public static final int CAAC_GEN_LAST_ID = 2999999;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Nom de l'entreprise, de l'organisation ou de l'autorité.
	 * Est appelé dans certains contextes raison sociale ou raison de commerce.
	 * Par exemple, "Soladest SA" ou "Département fédéral des finances"
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8elx9Edygsbnw9h5bVw"
	 */
	private String nom;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the nom
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8elx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "AC_NOM", length = LengthConstants.TIERS_NOM)
	public String getNom() {
		// begin-user-code
		return nom;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNom the nom to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8elx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNom(String theNom) {
		// begin-user-code
		nom = theNom;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8fFx9Edygsbnw9h5bVw"
	 */
	private FormeJuridique formeJuridique;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the formeJuridique
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8fFx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "AC_FORME_JURIDIQUE", length = LengthConstants.AC_FORME)
	@Type(type = "ch.vd.uniregctb.hibernate.FormeJuridiqueUserType")
	public FormeJuridique getFormeJuridique() {
		// begin-user-code
		return formeJuridique;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theFormeJuridique the formeJuridique to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8fFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setFormeJuridique(FormeJuridique theFormeJuridique) {
		// begin-user-code
		formeJuridique = theFormeJuridique;
		// end-user-code
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Autre tiers";
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.AutreCommunaute;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.AUTRE_COMMUNAUTE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final AutreCommunaute other = (AutreCommunaute) obj;
		return ComparisonHelper.areEqual(formeJuridique, other.formeJuridique)
				&& ComparisonHelper.areEqual(nom, other.nom);
	}
}

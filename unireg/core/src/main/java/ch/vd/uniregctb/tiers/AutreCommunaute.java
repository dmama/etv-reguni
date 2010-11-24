package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
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
public class AutreCommunaute extends Contribuable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4939991198494166708L;

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

	@Override
	public ValidationResults validate() {
		ValidationResults results = super.validate();
		
		if (isAnnule()) {
			return results;
		}

		if (nom == null || nom.equals("")) {
			results.addError("Le nom est un attribut obligatoire");
		}

		return results;
	}

	@Override
	protected ValidationResults validateTypeAdresses() {

		ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError("L'adresse de type 'personne morale' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur une autre communauté.");
				}
				else if (a instanceof AdresseCivile) {
					results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur une autre communauté.");
				}
			}
		}

		return results;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Autre tiers";
	}

	@Transient
	@Override
	public String getNatureTiers() {
		return AutreCommunaute.class.getSimpleName();
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
		AutreCommunaute other = (AutreCommunaute) obj;
		if (formeJuridique == null) {
			if (other.formeJuridique != null)
				return false;
		}
		else if (formeJuridique != other.formeJuridique)
			return false;
		if (nom == null) {
			if (other.nom != null)
				return false;
		}
		else if (!nom.equals(other.nom))
			return false;
		return true;
	}
}

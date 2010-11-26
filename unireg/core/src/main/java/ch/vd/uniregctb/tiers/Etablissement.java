package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Set;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * Installation matérielle au moyen de laquelle s'exerce une part de l'activité d'une entreprise (succursale, agence, dépôt), connu du registre des personnes morales de l'ACI.
 * Une raison individuelle est l'établissement d'une personne physqiue.
 * L'établissement en lui-même n'est pas contribuable, mais peut être constitutif d'un for fiscal (établissement stable).
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8clx9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8clx9Edygsbnw9h5bVw"
 */
@Entity
@DiscriminatorValue("Etablissement")
public class Etablissement extends Contribuable {
	/**
	 *
	 */
	private static final long serialVersionUID = 8778344268317960760L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Référence unique de l'établissement dans le registre des personnes morales (établissement d'une entreprise contribuable)
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8dVx9Edygsbnw9h5bVw"
	 */
	private Long numeroEtablissement;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroEtablissement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8dVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NUMERO_ETABLISSEMENT")
	public Long getNumeroEtablissement() {
		// begin-user-code
		return numeroEtablissement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroEtablissement the numeroEtablissement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8dVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroEtablissement(Long theNumeroEtablissement) {
		// begin-user-code
		numeroEtablissement = theNumeroEtablissement;
		// end-user-code
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Contribuable PM";
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.Etablissement;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.ETABLISSEMENT;
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
				if (a instanceof AdresseCivile) {
					results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur un établissement.");
				}
			}
		}

		return results;
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
		Etablissement other = (Etablissement) obj;
		if (numeroEtablissement == null) {
			if (other.numeroEtablissement != null)
				return false;
		}
		else if (!numeroEtablissement.equals(other.numeroEtablissement))
			return false;
		return true;
	}
}

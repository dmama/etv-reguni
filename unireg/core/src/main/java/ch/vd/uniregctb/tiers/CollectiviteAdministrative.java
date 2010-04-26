package ch.vd.uniregctb.tiers;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;

/**
 *
 */

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7BuAMPC-Edy2ztXteGM8AA"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7BuAMPC-Edy2ztXteGM8AA"
 */
@Entity
@DiscriminatorValue("CollectiviteAdministrative")
public class CollectiviteAdministrative extends Contribuable {

	/**
	 *
	 */
	private static final long serialVersionUID = -3901035764846227011L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JySRgPC_Edy2ztXteGM8AA"
	 */
	private Integer numeroCollectiviteAdministrative;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroCollectiviteAdministrative
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JySRgPC_Edy2ztXteGM8AA?GETTER"
	 */
	@Column(name = "NUMERO_CA", unique = true)
	//@Index(name = "IDX_NUMERO_CA") -> impliqué par le 'unique = true'
	public Integer getNumeroCollectiviteAdministrative() {
		// begin-user-code
		return numeroCollectiviteAdministrative;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroCollectiviteAdministrative the numeroCollectiviteAdministrative to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JySRgPC_Edy2ztXteGM8AA?SETTER"
	 */
	public void setNumeroCollectiviteAdministrative(Integer theNumeroCollectiviteAdministrative) {
		// begin-user-code
		numeroCollectiviteAdministrative = theNumeroCollectiviteAdministrative;
		// end-user-code
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Collectivité admin.";
	}

	@Transient
	@Override
	public String getNatureTiers() {
		return CollectiviteAdministrative.class.getSimpleName();
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
							+ a.getDateFin() + ") n'est pas autorisée sur une collectivité administrative.");
				}
				else if (a instanceof AdresseCivile) {
					results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur une collectivité administrative.");
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
		CollectiviteAdministrative other = (CollectiviteAdministrative) obj;
		if (numeroCollectiviteAdministrative == null) {
			if (other.numeroCollectiviteAdministrative != null)
				return false;
		}
		else if (!numeroCollectiviteAdministrative.equals(other.numeroCollectiviteAdministrative))
			return false;
		return true;
	}
}

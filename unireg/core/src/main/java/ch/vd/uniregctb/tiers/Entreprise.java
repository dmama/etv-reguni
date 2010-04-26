package ch.vd.uniregctb.tiers;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;

/**
 * Entreprise ou l'etablissement connue du registre des personnes morales de
 * l'ACI
 */
@Entity
@DiscriminatorValue("Entreprise")
public class Entreprise extends Contribuable {

	private static final long serialVersionUID = -5726364867046771919L;

	// Numéros migrés depuis SIMPA-PM
	public static final int FIRST_ID = 1;

	public static final int LAST_ID = 999999;

	// Numéros générés pour AutreCommunauté et CollectiviteAdministrative
	public static final int PM_GEN_FIRST_ID = 2000000;

	public static final int PM_GEN_LAST_ID = 2999999;

	private Long numeroEntreprise;

	@Column(name = "NUMERO_PM")
	public Long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public void setNumeroEntreprise(Long theNumeroEntreprise) {
		this.numeroEntreprise = theNumeroEntreprise;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Contribuable PM";
	}

	@Transient
	@Override
	public String getNatureTiers() {
		return Entreprise.class.getSimpleName();
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
							+ a.getDateFin() + ") n'est pas autorisée sur une entreprise.");
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
		Entreprise other = (Entreprise) obj;
		if (numeroEntreprise == null) {
			if (other.numeroEntreprise != null)
				return false;
		}
		else if (!numeroEntreprise.equals(other.numeroEntreprise))
			return false;
		return true;
	}
}

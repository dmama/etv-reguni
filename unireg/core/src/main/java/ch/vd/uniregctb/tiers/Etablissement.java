package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

/**
 * Installation matérielle au moyen de laquelle s'exerce une part de l'activité d'une entreprise (succursale, agence, dépôt), connu du registre des personnes morales de l'ACI.
 * Une raison individuelle est l'établissement d'une personne physqiue.
 * L'établissement en lui-même n'est pas contribuable, mais peut être constitutif d'un for fiscal (établissement stable).
 */
@Entity
@DiscriminatorValue("Etablissement")
public class Etablissement extends Contribuable {

	// Numéros (de tiers) générés pour les établissements
	public static final int ETB_GEN_FIRST_ID = 3000000;
	public static final int ETB_GEN_LAST_ID = 3999999;

	/**
	 * Identifiant cantonal (= dans RCEnt)
	 */
	private Long numeroEtablissement;

	@Column(name = "NUMERO_ETABLISSEMENT")
	@Index(name = "IDX_TIERS_NO_ETABLISSEMENT")
	public Long getNumeroEtablissement() {
		return numeroEtablissement;
	}

	public void setNumeroEtablissement(Long theNumeroEtablissement) {
		numeroEtablissement = theNumeroEtablissement;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Etablissement";
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

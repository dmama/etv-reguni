package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +----------------+                   +------------------+
 *   | Contribuable   |                   | Etablissement    |
 *   +----------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------| ActiviteEconomique |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("ActiviteEconomique")
public class ActiviteEconomique extends RapportEntreTiers {

	private static final String PERSONNE = "personne";
	private static final String ETABLISSEMENT = "établissement";

	public ActiviteEconomique() {
		// empty
	}

	public ActiviteEconomique(RegDate dateDebut, RegDate dateFin, Contribuable personneMoraleOuPhysique, Etablissement etablissement) {
		super(dateDebut, dateFin, personneMoraleOuPhysique, etablissement);
	}

	public ActiviteEconomique(ActiviteEconomique annuleEtRemplace) {
		super(annuleEtRemplace);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return ETABLISSEMENT;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return PERSONNE;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new ActiviteEconomique(this);
	}

}

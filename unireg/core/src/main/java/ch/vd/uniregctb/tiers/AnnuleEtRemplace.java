package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +----------------+                   +------------------+
 *   | Tiers Remplacé |                   | Tiers Remplaçant |
 *   +----------------+                   +------------------+
 *           ^                                      ^
 *           ¦  sujet  +------------------+  objet  ¦
 *           +---------| AnnuleEtRemplace |---------+
 *                     +------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("AnnuleEtRemplace")
public class AnnuleEtRemplace extends RapportEntreTiers {

	private static final String REMPLACE = "remplacé";
	private static final String REMPLACANT = "remplaçant";

	private static final long serialVersionUID = -1688097858774925185L;

	public AnnuleEtRemplace() {
		// empty
	}

	public AnnuleEtRemplace(RegDate dateDebut, RegDate dateFin, Tiers tiersRemplace, Tiers TiersRemplacant) {
		super(dateDebut, dateFin, tiersRemplace, TiersRemplacant);
	}

	public AnnuleEtRemplace(AnnuleEtRemplace annuleEtRemplace) {
		super(annuleEtRemplace);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.ANNULE_ET_REMPLACE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return REMPLACANT;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return REMPLACE;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new AnnuleEtRemplace(this);
	}

}

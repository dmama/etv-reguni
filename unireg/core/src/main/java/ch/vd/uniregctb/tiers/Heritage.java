package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +------------------+
 *   |     Héritier     |                   |      Défunt      |
 *   +------------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|     Héritage       |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Heritage")
public class Heritage extends RapportEntreTiers {

	private static final String HERITIER = "héritier";
	private static final String DEFUNT = "défunt(e)";

	public Heritage() {
	}

	public Heritage(RegDate dateDebut, RegDate dateFin, PersonnePhysique heritier, PersonnePhysique defunt) {
		super(dateDebut, dateFin, heritier, defunt);
	}

	protected Heritage(Heritage heritage) {
		super(heritage);
	}

	@Override
	public Heritage duplicate() {
		return new Heritage(this);
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return DEFUNT;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return HERITIER;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.HERITAGE;
	}
}

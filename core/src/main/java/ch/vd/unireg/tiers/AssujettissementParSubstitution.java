package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +------------------+                                                    +------------------+
 * | PersonnePhysique | (Substitué)                          (Substituant) | PersonnePhysique |
 * +------------------+                                                    +------------------+
 *         ^                                                                       ^
 *         ¦  sujet          +---------------------------------+            objet  ¦
 *         +-----------------|AssujettissementParSubstitution  |-------------------+
 *                           +---------------------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("AssujettissementParSubstitution")
public class AssujettissementParSubstitution extends RapportEntreTiers {

	private static final String SUBSTITUE = "substitué";
	private static final String SUBSTITUANT = "substituant";
	public AssujettissementParSubstitution() {
	}

	public AssujettissementParSubstitution(AssujettissementParSubstitution assujettissementParSubstitution) {
		super(assujettissementParSubstitution);
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return SUBSTITUANT;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return SUBSTITUE;
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new AssujettissementParSubstitution(this);
	}
}

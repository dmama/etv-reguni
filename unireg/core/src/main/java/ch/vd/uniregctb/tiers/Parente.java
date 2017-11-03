package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +------------------+                       +------------------+
 * | PersonnePhysique | (enfant)     (parent) | PersonnePhysique |
 * +------------------+                       +------------------+
 *         ^                                           ^
 *         ¦  sujet         +-----------+       objet  ¦
 *         +----------------|  Parenté  |--------------+
 *                          +-----------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Parente")
public class Parente extends RapportEntreTiers {

	private static final String PARENT = "parent";
	private static final String ENFANT = "enfant";

	public Parente() {
	}

	public Parente(RegDate dateDebut, RegDate dateFin, PersonnePhysique parent, PersonnePhysique enfant) {
		super(dateDebut, dateFin, enfant, parent);
	}

	private Parente(Parente rapport) {
		super(rapport);
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return PARENT;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return ENFANT;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.PARENTE;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new Parente(this);
	}
}

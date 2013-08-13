package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +------------------+                       +------------------+
 * | PersonnePhysique | (parent)     (enfant) | PersonnePhysique |
 * +------------------+                       +------------------+
 *         ^                                           ^
 *         ¦  sujet         +-----------+       objet  ¦
 *         +----------------| Filiation |--------------+
 *                          +-----------+
 * </pre>
 * <!-- end-user-doc -->
 */
@Entity
@DiscriminatorValue("Filiation")
public class Filiation extends RapportEntreTiers {

	private static final String PARENT = "parent";
	private static final String ENFANT = "enfant";

	public Filiation() {
	}

	public Filiation(RegDate dateDebut, RegDate dateFin, PersonnePhysique parent, PersonnePhysique enfant) {
		super(dateDebut, dateFin, parent, enfant);
	}

	public Filiation(RapportEntreTiers rapport) {
		super(rapport);
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return ENFANT;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return PARENT;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.FILIATION;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new Filiation(this);
	}
}

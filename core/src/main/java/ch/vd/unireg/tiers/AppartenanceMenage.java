package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +------------------+                     +--------------+
 * | PersonnePhysique |                     | MenageCommun |
 * +------------------+                     +--------------+
 *         ^                                        ^
 *         ¦  sujet  +--------------------+  objet  ¦
 *         +---------| AppartenanceMenage |---------+
 *                   +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("AppartenanceMenage")
public class AppartenanceMenage extends RapportEntreTiers {

	private static final String PERSONNE_PHYSIQUE = "personne physique";
	private static final String MENAGE_COMMUN = "ménage commun";

	public AppartenanceMenage() {
		// empty
	}

	public AppartenanceMenage(AppartenanceMenage representation) {
		super(representation);
	}

	public AppartenanceMenage(RegDate dateDebut, RegDate dateFin, PersonnePhysique pp, MenageCommun menage) {
		super(dateDebut, dateFin, pp, menage);
	}

	/* (non-Javadoc)
	 * @see ch.vd.unireg.tiers.RapportEntreTiers#duplicate()
	 */
	@Override
	public RapportEntreTiers duplicate() {
		return new AppartenanceMenage(this);
	}

	@Override
	@Transient
	public final TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.APPARTENANCE_MENAGE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return MENAGE_COMMUN;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return PERSONNE_PHYSIQUE;
	}
}
package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +------------------+                     +--------------+
 * | PersonnePhysique |                     | MenageCommun |
 * +------------------+                     +--------------+
 *         ^                                        ^
 *         ¦  sujet  +--------------------+  objet  ¦
 *         +---------| AppartenanceMenage |---------+
 *                   +--------------------+
 * </pre>
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZV_O4BFLEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZV_O4BFLEd2nzO4G1YQacw"
 */
@Entity
@DiscriminatorValue("AppartenanceMenage")
public class AppartenanceMenage extends RapportEntreTiers {

	private static final long serialVersionUID = 7917054720448439223L;

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
	 * @see ch.vd.uniregctb.tiers.RapportEntreTiers#duplicate()
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
}
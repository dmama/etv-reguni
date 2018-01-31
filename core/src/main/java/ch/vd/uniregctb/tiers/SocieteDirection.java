package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +--------------------+
 *   | Propiétaire      |                   | Fonds de placement |
 *   +------------------+                   +--------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|  SocieteDirection  |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("SocieteDirection")
public class SocieteDirection extends RapportEntreTiers {

	public static final String PROPRIETAIRE = "propriétaire";
	public static final String FONDS = "fonds de placement";

	public SocieteDirection() {
	}

	public SocieteDirection(RegDate dateDebut, RegDate dateFin, Entreprise proprietaire, Entreprise fonds) {
		super(dateDebut, dateFin, proprietaire, fonds);
	}

	protected SocieteDirection(SocieteDirection src) {
		super(src);
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return FONDS;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return PROPRIETAIRE;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.SOCIETE_DIRECTION;
	}

	@Override
	public SocieteDirection duplicate() {
		return new SocieteDirection(this);
	}
}

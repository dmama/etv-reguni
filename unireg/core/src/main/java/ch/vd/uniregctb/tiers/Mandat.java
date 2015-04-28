package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +----------------+                   +------------------+
 *   |    Mandant     |                   |    Mandataire    |
 *   +----------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|      Mandat        |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Mandat")
public class Mandat extends RapportEntreTiers {

	private static final String MANDANT = "mandant";
	private static final String MANDATAIRE = "mandataire";

	// TODO il manque le type de mandat

	public Mandat() {
		// empty
	}

	public Mandat(RegDate dateDebut, RegDate dateFin, Contribuable mandant, Contribuable mandataire) {
		super(dateDebut, dateFin, mandant, mandataire);
	}

	protected Mandat(Mandat src) {
		super(src);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.MANDAT;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return MANDATAIRE;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return MANDANT;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new Mandat(this);
	}

}

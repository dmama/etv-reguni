package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +------------------+
 *   | Entreprise avant |                   | Entreprise après |
 *   +------------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|      Scission      |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("ScissionEntreprise")
public class ScissionEntreprise extends RapportEntreTiers {

	private static final String AVANT = "entreprise avant scission";
	private static final String APRES = "entreprise après scission";

	public ScissionEntreprise() {
		// empty
	}

	public ScissionEntreprise(RegDate dateDebut, RegDate dateFin, Entreprise avant, Entreprise apres) {
		super(dateDebut, dateFin, avant, apres);
	}

	protected ScissionEntreprise(ScissionEntreprise src) {
		super(src);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.SCISSION_ENTREPRISE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return APRES;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return AVANT;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new ScissionEntreprise(this);
	}

}

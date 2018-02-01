package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +------------------+
 *   | Entreprise avant |                   | Entreprise après |
 *   +------------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|      Fusion        |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("FusionEntreprises")
public class FusionEntreprises extends RapportEntreTiers {

	private static final String AVANT = "entreprise avant fusion";
	private static final String APRES = "entreprise après fusion";

	public FusionEntreprises() {
		// empty
	}

	public FusionEntreprises(RegDate dateDebut, RegDate dateFin, Entreprise avant, Entreprise apres) {
		super(dateDebut, dateFin, avant, apres);
	}

	protected FusionEntreprises(FusionEntreprises src) {
		super(src);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.FUSION_ENTREPRISES;
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
		return new FusionEntreprises(this);
	}

}

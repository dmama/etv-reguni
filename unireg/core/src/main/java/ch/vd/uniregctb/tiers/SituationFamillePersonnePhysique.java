package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("SituationFamille")
public class SituationFamillePersonnePhysique extends SituationFamille {

	private static final long serialVersionUID = 474627638437L;

	public SituationFamillePersonnePhysique() {
	}

	public SituationFamillePersonnePhysique(SituationFamillePersonnePhysique situationFamille) {
		super(situationFamille);
	}

	public SituationFamille duplicate() {
		return new SituationFamillePersonnePhysique(this);
	}
}

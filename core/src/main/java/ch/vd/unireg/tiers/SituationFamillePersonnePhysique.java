package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("SituationFamille")
public class SituationFamillePersonnePhysique extends SituationFamille {

	public SituationFamillePersonnePhysique() {
	}

	public SituationFamillePersonnePhysique(SituationFamillePersonnePhysique situationFamille) {
		super(situationFamille);
	}

	@Override
	public SituationFamille duplicate() {
		return new SituationFamillePersonnePhysique(this);
	}
}

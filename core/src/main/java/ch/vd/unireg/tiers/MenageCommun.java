package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue("MenageCommun")
public class MenageCommun extends ContribuableImpositionPersonnesPhysiques {

    @Transient
    @Override
    public NatureTiers getNatureTiers() {
        return NatureTiers.MenageCommun;
    }

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.MENAGE_COMMUN;
	}
}

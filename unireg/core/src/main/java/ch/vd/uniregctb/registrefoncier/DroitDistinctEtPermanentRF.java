package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DroitDistinctEtPermanent")
public class DroitDistinctEtPermanentRF extends ImmeubleRF {
}

package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Mine")
public class MineRF extends ImmeubleRF {
}

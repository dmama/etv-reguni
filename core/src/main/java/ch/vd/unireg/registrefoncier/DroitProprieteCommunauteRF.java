package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DroitProprieteCommunaute")
public class DroitProprieteCommunauteRF extends DroitProprieteRF {
}

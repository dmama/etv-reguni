package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DroitProprietePP")
public class DroitProprietePersonnePhysiqueRF extends DroitProprietePersonneRF {

}

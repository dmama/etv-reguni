package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DroitProprietePM")
public class DroitProprietePersonneMoraleRF extends DroitProprietePersonneRF {

}

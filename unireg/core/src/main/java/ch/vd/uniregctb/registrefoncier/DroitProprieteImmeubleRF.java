package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Droit de propriété entre un immeuble bénéficiaire et un immeuble grevé.
 */
@Entity
@DiscriminatorValue("DroitProprieteImmeuble")
public class DroitProprieteImmeubleRF extends DroitProprieteRF {
}

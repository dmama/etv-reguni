package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("PartCopropriete")
public class PartCoproprieteRF extends ImmeubleAvecQuotePartRF {

}

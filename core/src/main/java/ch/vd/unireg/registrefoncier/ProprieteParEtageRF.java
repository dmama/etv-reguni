package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ProprieteParEtage")
public class ProprieteParEtageRF extends ImmeubleAvecQuotePartRF {

}

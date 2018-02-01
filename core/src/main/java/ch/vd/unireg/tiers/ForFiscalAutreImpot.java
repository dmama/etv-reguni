package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ForFiscalAutreImpot")
public class ForFiscalAutreImpot extends ForFiscal {

	public ForFiscalAutreImpot() {
	}

	public ForFiscalAutreImpot(ForFiscalAutreImpot ffai) {
		super(ffai);
	}

	@Override
	public ForFiscal duplicate() {
		return new ForFiscalAutreImpot(this);
	}

}

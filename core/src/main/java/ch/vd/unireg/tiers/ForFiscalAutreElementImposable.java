package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ForFiscalAutreElementImposable")
public class ForFiscalAutreElementImposable extends ForFiscalRevenuFortune {

	public ForFiscalAutreElementImposable() {
	}

	public ForFiscalAutreElementImposable(ForFiscalAutreElementImposable ffaei) {
		super(ffaei);
	}

	@Override
	public ForFiscal duplicate() {
		return new ForFiscalAutreElementImposable(this);
	}
}

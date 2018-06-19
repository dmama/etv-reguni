package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.equalator;

import ch.vd.evd0022.v3.Capital;
import ch.vd.unireg.common.Equalator;

public class CapitalEqualator implements Equalator<Capital> {

	@Override
	 public boolean test(Capital c1, Capital c2) {
		if (c1 == c2) {
			return true;
		}
		if (c1 == null || c2 == null || c1.getClass() != c2.getClass()) {
			return false;
		}

		if (c1.getTypeOfCapital() != c2.getTypeOfCapital()) return false;
		if (c1.getCurrency() != null ? !c1.getCurrency().equals(c2.getCurrency()) : c2.getCurrency() != null) return false;
		if (c1.getCapitalAmount() != null ? !c1.getCapitalAmount().equals(c2.getCapitalAmount()) : c2.getCapitalAmount() != null) return false;
		if (c1.getCashedInAmount() != null ? !c1.getCashedInAmount().equals(c2.getCashedInAmount()) : c2.getCashedInAmount() != null) return false;
		return !(c1.getDivision() != null ? !c1.getDivision().equals(c2.getDivision()) : c2.getDivision() != null);
	}
}

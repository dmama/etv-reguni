package ch.vd.uniregctb.adapter.rcent.historizer.equalator;

import ch.vd.uniregctb.adapter.rcent.model.Function;

public class FunctionEqualator implements Equalator<ch.vd.evd0022.v3.Function> {

	@Override
	public boolean test(ch.vd.evd0022.v3.Function f1, ch.vd.evd0022.v3.Function f2) {
		if (f1 == f2) return true;
		if (f2 == null || f1.getClass() != f2.getClass()) return false;

		return new Function(f1).equals(new Function(f2));
	}
}

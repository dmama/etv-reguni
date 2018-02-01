package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;

import ch.vd.evd0022.v3.Function;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.OrganisationFunction;
import ch.vd.unireg.common.Equalator;

public class OrganisationFunctionEqualator implements Equalator<Function> {

	@Override
	public boolean test(Function f1, Function f2) {
		if (f1 == f2) return true;
		if (f2 == null || f1.getClass() != f2.getClass()) return false;

		return new OrganisationFunction(f1).equals(new OrganisationFunction(f2));
	}
}

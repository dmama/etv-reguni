package ch.vd.uniregctb.migration.pm.rcent.model.wrappers;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedWrapper;

public class RCEntIdentifier extends RCEntRangedWrapper<Identifier> {

	public RCEntIdentifier(RegDate beginDate, RegDate endDateDate, Identifier element) {
		super(beginDate, endDateDate, element);
	}

	public String getIdentifierCategory() {
		return getElement().getIdentifierCategory();
	}

	public String getIdentifierValue() {
		return getElement().getIdentifierValue();
	}

}

package ch.vd.uniregctb.migration.pm.rcent.model.wrappers;

import ch.vd.evd0022.v1.Authorisation;
import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Party;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedWrapper;

public class RCEntFunction  extends RCEntRangedWrapper<Function> {

	public RCEntFunction(RegDate beginDate, RegDate endDateDate, Function element) {
		super(beginDate, endDateDate, element);
	}

	public Authorisation getAuthorisation() {
		return getElement().getAuthorisation();
	}

	public String getAuthorisationRestriction() {
		return getElement().getAuthorisationRestriction();
	}

	public Party getParty() {
		return getElement().getParty();
	}

	public String getFunctionText() {
		return getElement().getFunctionText();
	}
}

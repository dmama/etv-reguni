package ch.vd.uniregctb.migration.pm.rcent.model.organisation;

import ch.vd.evd0021.v1.Address;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryElement;

public class RCEntAddress extends RCEntHistoryElement {

	private Address address;

	public RCEntAddress(RegDate beginDate, RegDate endDateDate, Address address) {
		super(beginDate, endDateDate);
		this.address = address;
	}
}

package ch.vd.uniregctb.migration.pm.rcent.model;

import ch.vd.evd0021.v1.Address;
import ch.vd.registre.base.date.RegDate;

public class RCEntAddress extends RCEntAbstractHistoryElement {

	private Address address;

	public RCEntAddress(RegDate beginDate, RegDate endDateDate, Address address) {
		super(beginDate, endDateDate);
		this.address = address;
	}
}

package ch.vd.uniregctb.migration.pm.rcent.model.main;

import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntListOfRanges;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedValue;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntAddress;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntCapitalRC;

public class RCEntRCData {
	private final RCEntListOfRanges<RCEntRangedValue<CommercialRegisterStatus>> RCStatus;
	private final RCEntListOfRanges<RCEntRangedValue<String>> RCName;
	private final RCEntListOfRanges<RCEntRangedValue<CommercialRegisterEntryStatus>> RCEntryStatus;
	private final RCEntListOfRanges<RCEntCapitalRC> RCCapital;
	private final RCEntListOfRanges<RCEntAddress> RCLegalAddress;
	private final RCEntListOfRanges<RCEntRangedValue> RCEntryDate;
	private final RCEntListOfRanges<RCEntRangedValue> RCCancellationDate;

	public RCEntRCData(RCEntListOfRanges<RCEntRangedValue> RCCancellationDate,
	                   RCEntListOfRanges<RCEntCapitalRC> RCCapital,
	                   RCEntListOfRanges<RCEntRangedValue> RCEntryDate,
	                   RCEntListOfRanges<RCEntRangedValue<CommercialRegisterEntryStatus>> RCEntryStatus,
	                   RCEntListOfRanges<RCEntAddress> RCLegalAddress,
	                   RCEntListOfRanges<RCEntRangedValue<String>> RCName,
	                   RCEntListOfRanges<RCEntRangedValue<CommercialRegisterStatus>> RCStatus) {
		this.RCCancellationDate = RCCancellationDate;
		this.RCCapital = RCCapital;
		this.RCEntryDate = RCEntryDate;
		this.RCEntryStatus = RCEntryStatus;
		this.RCLegalAddress = RCLegalAddress;
		this.RCName = RCName;
		this.RCStatus = RCStatus;
	}

	public RCEntListOfRanges<RCEntRangedValue> getRCCancellationDate() {
		return RCCancellationDate;
	}

	public RCEntListOfRanges<RCEntCapitalRC> getRCCapital() {
		return RCCapital;
	}

	public RCEntListOfRanges<RCEntRangedValue> getRCEntryDate() {
		return RCEntryDate;
	}

	public RCEntListOfRanges<RCEntRangedValue<CommercialRegisterEntryStatus>> getRCEntryStatus() {
		return RCEntryStatus;
	}

	public RCEntListOfRanges<RCEntAddress> getRCLegalAddress() {
		return RCLegalAddress;
	}

	public RCEntListOfRanges<RCEntRangedValue<String>> getRCName() {
		return RCName;
	}

	public RCEntListOfRanges<RCEntRangedValue<CommercialRegisterStatus>> getRCStatus() {
		return RCStatus;
	}
}

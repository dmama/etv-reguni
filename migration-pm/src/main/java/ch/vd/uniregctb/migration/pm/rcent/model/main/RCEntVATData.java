package ch.vd.uniregctb.migration.pm.rcent.model.main;

import ch.vd.evd0022.v1.VatRegisterEntryStatus;
import ch.vd.evd0022.v1.VatRegisterStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntListOfRanges;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedValue;

public class RCEntVATData {

	private final RCEntListOfRanges<RCEntRangedValue<VatRegisterStatus>> VATStatus;
	private final RCEntListOfRanges<RCEntRangedValue<VatRegisterEntryStatus>> VATEntryStatus;
	private final RCEntListOfRanges<RCEntRangedValue<RegDate>> VATEntryDate;
	private final RCEntListOfRanges<RCEntRangedValue<RegDate>> VATCancellationDate;

	public RCEntVATData(
			RCEntListOfRanges<RCEntRangedValue<RegDate>> VATCancellationDate,
			RCEntListOfRanges<RCEntRangedValue<VatRegisterStatus>> VATStatus,
			RCEntListOfRanges<RCEntRangedValue<VatRegisterEntryStatus>> VATEntryStatus,
			RCEntListOfRanges<RCEntRangedValue<RegDate>> VATEntryDate) {
		this.VATCancellationDate = VATCancellationDate;
		this.VATStatus = VATStatus;
		this.VATEntryStatus = VATEntryStatus;
		this.VATEntryDate = VATEntryDate;
	}

	public RCEntListOfRanges<RCEntRangedValue<RegDate>> getVATCancellationDate() {
		return VATCancellationDate;
	}

	public RCEntListOfRanges<RCEntRangedValue<RegDate>> getVATEntryDate() {
		return VATEntryDate;
	}

	public RCEntListOfRanges<RCEntRangedValue<VatRegisterEntryStatus>> getVATEntryStatus() {
		return VATEntryStatus;
	}

	public RCEntListOfRanges<RCEntRangedValue<VatRegisterStatus>> getVATStatus() {
		return VATStatus;
	}
}

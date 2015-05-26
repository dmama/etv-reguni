package ch.vd.uniregctb.migration.pm.rcent.model.main;

import ch.vd.evd0022.v1.UidRegisterLiquidationReason;
import ch.vd.evd0022.v1.UidRegisterPublicStatus;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntListOfRanges;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedValue;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntAddress;

public class RCEntUIDData {
	private final RCEntListOfRanges<RCEntRangedValue<UidRegisterStatus>> UIDStatus;
	private final RCEntListOfRanges<RCEntRangedValue<UidRegisterTypeOfOrganisation>> UIDTypeOfOrganisation;
	private final RCEntListOfRanges<RCEntAddress> UIDEffectiveAddress;
	private final RCEntListOfRanges<RCEntAddress> UIDPostOfficeBoxAddress;
	private final RCEntListOfRanges<RCEntRangedValue<UidRegisterPublicStatus>> UIDPublicStatus;
	private final RCEntListOfRanges<RCEntRangedValue<UidRegisterLiquidationReason>> UIDLiquidationReason;

	public RCEntUIDData(RCEntListOfRanges<RCEntAddress> UIDEffectiveAddress,
	                    RCEntListOfRanges<RCEntRangedValue<UidRegisterStatus>> UIDStatus,
	                    RCEntListOfRanges<RCEntRangedValue<UidRegisterTypeOfOrganisation>> UIDTypeOfOrganisation,
	                    RCEntListOfRanges<RCEntAddress> UIDPostOfficeBoxAddress,
	                    RCEntListOfRanges<RCEntRangedValue<UidRegisterPublicStatus>> UIDPublicStatus,
	                    RCEntListOfRanges<RCEntRangedValue<UidRegisterLiquidationReason>> UIDLiquidationReason) {
		this.UIDEffectiveAddress = UIDEffectiveAddress;
		this.UIDStatus = UIDStatus;
		this.UIDTypeOfOrganisation = UIDTypeOfOrganisation;
		this.UIDPostOfficeBoxAddress = UIDPostOfficeBoxAddress;
		this.UIDPublicStatus = UIDPublicStatus;
		this.UIDLiquidationReason = UIDLiquidationReason;
	}

	public RCEntListOfRanges<RCEntAddress> getUIDEffectiveAddress() {
		return UIDEffectiveAddress;
	}

	public RCEntListOfRanges<RCEntRangedValue<UidRegisterLiquidationReason>> getUIDLiquidationReason() {
		return UIDLiquidationReason;
	}

	public RCEntListOfRanges<RCEntAddress> getUIDPostOfficeBoxAddress() {
		return UIDPostOfficeBoxAddress;
	}

	public RCEntListOfRanges<RCEntRangedValue<UidRegisterPublicStatus>> getUIDPublicStatus() {
		return UIDPublicStatus;
	}

	public RCEntListOfRanges<RCEntRangedValue<UidRegisterStatus>> getUIDStatus() {
		return UIDStatus;
	}

	public RCEntListOfRanges<RCEntRangedValue<UidRegisterTypeOfOrganisation>> getUIDTypeOfOrganisation() {
		return UIDTypeOfOrganisation;
	}
}

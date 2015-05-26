package ch.vd.uniregctb.migration.pm.rcent.model.main;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntListOfRanges;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedValue;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntCapitalRC;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntFunction;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntIdentification;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntIdentifier;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntSwissMunicipality;

public class RCEntOrganisationLocation {

    /**
     * Identifiant cantonal - "Clé primaire"
     */
    final private long cantonalId;
    @NotNull
    final private RCEntListOfRanges<RCEntRangedValue<String>> name;

	public final RCEntRCData rc;
	public final RCEntUIDData uid;
	public final RCEntVATData vat;

	public final RCEntListOfRanges<RCEntIdentifier> identifier;
	public final RCEntListOfRanges<RCEntRangedValue<String>> otherNames;
	public final RCEntListOfRanges<RCEntRangedValue<KindOfLocation>> kindOfLocation;
	public final RCEntListOfRanges<RCEntSwissMunicipality> seat;
	public final RCEntListOfRanges<RCEntFunction> function;
	public final RCEntListOfRanges<RCEntRangedValue<String>> nogaCode;
	public final RCEntListOfRanges<RCEntIdentification> replacedBy;
	public final RCEntListOfRanges<RCEntIdentification> inPreplacementOf;

	public RCEntOrganisationLocation(long cantonalId,
	                                 @NotNull RCEntListOfRanges<RCEntRangedValue<String>> name, RCEntListOfRanges<RCEntCapitalRC> capitalRC, RCEntRCData rc, RCEntUIDData uid, RCEntVATData vat,
	                                 RCEntListOfRanges<RCEntIdentifier> identifier, RCEntListOfRanges<RCEntRangedValue<String>> otherNames,
	                                 RCEntListOfRanges<RCEntRangedValue<KindOfLocation>> kindOfLocation, RCEntListOfRanges<RCEntSwissMunicipality> seat, RCEntListOfRanges<RCEntFunction> function,
	                                 RCEntListOfRanges<RCEntRangedValue<String>> nogaCode, RCEntListOfRanges<RCEntIdentification> replacedBy, RCEntListOfRanges<RCEntIdentification> inPreplacementOf) {
		this.cantonalId = cantonalId;
		this.name = name;
		this.rc = rc;
		this.uid = uid;
		this.vat = vat;
		this.identifier = identifier;
		this.otherNames = otherNames;
		this.kindOfLocation = kindOfLocation;
		this.seat = seat;
		this.function = function;
		this.nogaCode = nogaCode;
		this.replacedBy = replacedBy;
		this.inPreplacementOf = inPreplacementOf;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public RCEntListOfRanges<RCEntFunction> getFunction() {
		return function;
	}

	public RCEntListOfRanges<RCEntIdentifier> getIdentifier() {
		return identifier;
	}

	public RCEntListOfRanges<RCEntIdentification> getInPreplacementOf() {
		return inPreplacementOf;
	}

	public RCEntListOfRanges<RCEntRangedValue<KindOfLocation>> getKindOfLocation() {
		return kindOfLocation;
	}

	@NotNull
	public RCEntListOfRanges<RCEntRangedValue<String>> getName() {
		return name;
	}

	public RCEntListOfRanges<RCEntRangedValue<String>> getNogaCode() {
		return nogaCode;
	}

	public RCEntListOfRanges<RCEntRangedValue<String>> getOtherNames() {
		return otherNames;
	}

	public RCEntRCData getRc() {
		return rc;
	}

	public RCEntListOfRanges<RCEntIdentification> getReplacedBy() {
		return replacedBy;
	}

	public RCEntListOfRanges<RCEntSwissMunicipality> getSeat() {
		return seat;
	}

	public RCEntUIDData getUid() {
		return uid;
	}

	public RCEntVATData getVat() {
		return vat;
	}
}

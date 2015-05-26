package ch.vd.uniregctb.migration.pm.rcent.model.wrappers;

import ch.vd.evd0022.v1.CantonAbbreviation;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedWrapper;

public class RCEntSwissMunicipality extends RCEntRangedWrapper<SwissMunicipality> {

	public RCEntSwissMunicipality(RegDate beginDate, RegDate endDateDate, SwissMunicipality element) {
		super(beginDate, endDateDate, element);
	}

	public Integer getMunicipalityId() {
		return getElement().getMunicipalityId();
	}

	public String getMunicipalityName() {
		return getElement().getMunicipalityName();
	}

	public CantonAbbreviation getCantonAbbreviation() {
		return getElement().getCantonAbbreviation();
	}

	public Integer getHistoryMunicipalityId() {
		return getElement().getHistoryMunicipalityId();
	}
}

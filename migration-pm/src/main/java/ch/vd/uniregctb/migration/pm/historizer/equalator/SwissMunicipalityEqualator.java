package ch.vd.uniregctb.migration.pm.historizer.equalator;

import ch.vd.evd0022.v1.SwissMunicipality;

public class SwissMunicipalityEqualator implements Equalator<SwissMunicipality> {

	@Override
	public boolean test(SwissMunicipality m1, SwissMunicipality m2) {
		if (m1 == m2) return true;
		if (m2 == null || m1.getClass() != m2.getClass()) return false;

		if (m1.getMunicipalityId() != null ? !m1.getMunicipalityId().equals(m2.getMunicipalityId()) : m2.getMunicipalityId() != null) return false;
		if (!m1.getMunicipalityName().equals(m2.getMunicipalityName())) return false;
		if (m1.getCantonAbbreviation() != m2.getCantonAbbreviation()) return false;
		return !(m1.getHistoryMunicipalityId() != null ? !m1.getHistoryMunicipalityId().equals(m2.getHistoryMunicipalityId()) : m2.getHistoryMunicipalityId() != null);
	}
}

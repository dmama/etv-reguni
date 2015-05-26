package ch.vd.uniregctb.migration.pm.historizer.equalator;

import ch.vd.evd0022.v1.SwissMunicipality;

public class SeatEqualator implements Equalator<SwissMunicipality> {

	@Override
	public boolean test(SwissMunicipality m1, SwissMunicipality m2) {
		return m1 == m2 ||
				!(m1 == null || m2 == null || m1.getClass() != m2.getClass())
						&& m1.getMunicipalityId().equals(m2.getMunicipalityId());
	}
}

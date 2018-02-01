package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;

import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.BurRegistrationData;
import ch.vd.unireg.common.Equalator;

public class BURRegistrationDataEqualator implements Equalator<BurRegistrationData> {

	@Override
	public boolean test(BurRegistrationData burRegistrationData1, BurRegistrationData burRegistrationData2) {
		return burRegistrationData1.getRegistrationDate() == burRegistrationData2.getRegistrationDate() && burRegistrationData1.getStatus() == burRegistrationData2.getStatus();
	}
}

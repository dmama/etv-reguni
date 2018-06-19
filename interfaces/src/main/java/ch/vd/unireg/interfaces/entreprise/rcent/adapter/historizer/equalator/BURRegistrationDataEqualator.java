package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.equalator;

import ch.vd.unireg.common.Equalator;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.BurRegistrationData;

public class BURRegistrationDataEqualator implements Equalator<BurRegistrationData> {

	@Override
	public boolean test(BurRegistrationData burRegistrationData1, BurRegistrationData burRegistrationData2) {
		return burRegistrationData1.getRegistrationDate() == burRegistrationData2.getRegistrationDate() && burRegistrationData1.getStatus() == burRegistrationData2.getStatus();
	}
}

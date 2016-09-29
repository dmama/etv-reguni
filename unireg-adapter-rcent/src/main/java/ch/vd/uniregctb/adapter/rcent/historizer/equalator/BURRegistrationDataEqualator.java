package ch.vd.uniregctb.adapter.rcent.historizer.equalator;

import java.util.Objects;

import ch.vd.uniregctb.adapter.rcent.model.BurRegistrationData;

public class BURRegistrationDataEqualator implements Equalator<BurRegistrationData> {

	@Override
	public boolean test(BurRegistrationData burRegistrationData1, BurRegistrationData burRegistrationData2) {
		return Objects.equals(burRegistrationData1.getRegistrationDate(), burRegistrationData2.getRegistrationDate())
				&& Objects.equals(burRegistrationData1.getStatus(), burRegistrationData2.getStatus());
	}
}

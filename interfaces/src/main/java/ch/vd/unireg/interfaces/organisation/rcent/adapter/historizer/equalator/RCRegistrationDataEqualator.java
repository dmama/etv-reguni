package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;

import java.util.Objects;

import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.RCRegistrationData;
import ch.vd.unireg.common.Equalator;

public class RCRegistrationDataEqualator implements Equalator<RCRegistrationData> {

	@Override
	public boolean test(RCRegistrationData rcRegistrationData1, RCRegistrationData rcRegistrationData2) {
		return Objects.equals(rcRegistrationData1.getChDeregistrationDate(), rcRegistrationData2.getChDeregistrationDate())
				&& Objects.equals(rcRegistrationData1.getChRegistrationDate(), rcRegistrationData2.getChRegistrationDate())
				&& Objects.equals(rcRegistrationData1.getVdDeregistrationDate(), rcRegistrationData2.getVdDeregistrationDate())
				&& Objects.equals(rcRegistrationData1.getVdRegistrationDate(), rcRegistrationData2.getVdRegistrationDate())
				&& Objects.equals(rcRegistrationData1.getRegistrationStatus(), rcRegistrationData2.getRegistrationStatus())
				&& Objects.equals(rcRegistrationData1.getVdDissolutionReason(), rcRegistrationData2.getVdDissolutionReason());
	}
}

package ch.vd.uniregctb.adapter.rcent.model;

import ch.vd.evd0022.v3.BurLocalUnitStatus;
import ch.vd.registre.base.date.RegDate;

public class BurRegistrationData {

	private final BurLocalUnitStatus status;
	private final RegDate registrationDate;

	public BurRegistrationData(BurLocalUnitStatus status, RegDate registrationDate) {
		this.status = status;
		this.registrationDate = registrationDate;
	}

	public BurLocalUnitStatus getStatus() {
		return status;
	}

	public RegDate getRegistrationDate() {
		return registrationDate;
	}
}

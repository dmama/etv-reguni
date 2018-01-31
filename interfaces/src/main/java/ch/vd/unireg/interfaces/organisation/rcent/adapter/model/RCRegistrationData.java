package ch.vd.unireg.interfaces.organisation.rcent.adapter.model;

import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.registre.base.date.RegDate;

public class RCRegistrationData{

	private final CommercialRegisterStatus registrationStatus;
	private final RegDate chRegistrationDate;
	private final RegDate chDeregistrationDate;
	private final RegDate vdRegistrationDate;
	private final RegDate vdDeregistrationDate;
	private final DissolutionReason vdDissolutionReason;

	public RCRegistrationData(CommercialRegisterStatus registrationStatus,
	                          RegDate chRegistrationDate, RegDate chDeregistrationDate,
	                          RegDate vdRegistrationDate, RegDate vdDeregistrationDate,
	                          DissolutionReason vdDissolutionReason) {
		this.registrationStatus = registrationStatus;
		this.chRegistrationDate = chRegistrationDate;
		this.chDeregistrationDate = chDeregistrationDate;
		this.vdRegistrationDate = vdRegistrationDate;
		this.vdDeregistrationDate = vdDeregistrationDate;
		this.vdDissolutionReason = vdDissolutionReason;
	}

	public CommercialRegisterStatus getRegistrationStatus() {
		return registrationStatus;
	}

	public RegDate getChRegistrationDate() {
		return chRegistrationDate;
	}

	public RegDate getChDeregistrationDate() {
		return chDeregistrationDate;
	}

	public RegDate getVdRegistrationDate() {
		return vdRegistrationDate;
	}

	public RegDate getVdDeregistrationDate() {
		return vdDeregistrationDate;
	}

	public DissolutionReason getVdDissolutionReason() {
		return vdDissolutionReason;
	}
}

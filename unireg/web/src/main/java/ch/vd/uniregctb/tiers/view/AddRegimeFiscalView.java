package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class AddRegimeFiscalView {

	private long pmId;
	private RegimeFiscal.Portee portee;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String code;

	public AddRegimeFiscalView() {
	}

	public AddRegimeFiscalView(long pmId, RegimeFiscal.Portee portee) {
		this.pmId = pmId;
		this.portee = portee;
	}

	public long getPmId() {
		return pmId;
	}

	public void setPmId(long pmId) {
		this.pmId = pmId;
	}

	public RegimeFiscal.Portee getPortee() {
		return portee;
	}

	public void setPortee(RegimeFiscal.Portee portee) {
		this.portee = portee;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}

package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class EditRegimeFiscalView implements ValidableRegimeFiscalView {

	private long pmId;
	private long rfId;
	private RegimeFiscal.Portee portee;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String code;
	private TypeRegimeFiscal type;

	@SuppressWarnings("unused")
	public EditRegimeFiscalView() {
	}

	public EditRegimeFiscalView(RegimeFiscal rf, ServiceRegimeFiscal regimeFiscalService) {
		this.rfId = rf.getId();
		this.pmId = rf.getEntreprise().getNumero();
		this.portee = rf.getPortee();
		this.dateDebut = rf.getDateDebut();
		this.dateFin = rf.getDateFin();
		this.code = rf.getCode();
		this.type = regimeFiscalService.getTypeRegimeFiscal(rf.getCode());
	}

	public long getPmId() {
		return pmId;
	}

	public void setPmId(long pmId) {
		this.pmId = pmId;
	}

	public long getRfId() {
		return rfId;
	}

	public void setRfId(long rfId) {
		this.rfId = rfId;
	}

	@Override
	public RegimeFiscal.Portee getPortee() {
		return portee;
	}

	public void setPortee(RegimeFiscal.Portee portee) {
		this.portee = portee;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public TypeRegimeFiscal getType() {
		return type;
	}
}

package ch.vd.uniregctb.registrefoncier.communaute;

public class AddPrincipalView {

	private long modeleId;
	private long membreId;
	private Integer periodeDebut;

	public AddPrincipalView() {
	}

	public AddPrincipalView(long modeleId, long membreId, Integer periodeDebut) {
		this.modeleId = modeleId;
		this.membreId = membreId;
		this.periodeDebut = periodeDebut;
	}

	public long getModeleId() {
		return modeleId;
	}

	public void setModeleId(long modeleId) {
		this.modeleId = modeleId;
	}

	public long getMembreId() {
		return membreId;
	}

	public void setMembreId(long membreId) {
		this.membreId = membreId;
	}

	public Integer getPeriodeDebut() {
		return periodeDebut;
	}

	public void setPeriodeDebut(Integer periodeDebut) {
		this.periodeDebut = periodeDebut;
	}
}

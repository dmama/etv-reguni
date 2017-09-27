package ch.vd.uniregctb.registrefoncier.communaute;

import ch.vd.registre.base.date.RegDate;

public class AddPrincipalView {

	private long modeleId;
	private long membreId;
	private RegDate dateDebut;

	public AddPrincipalView() {
	}

	public AddPrincipalView(long modeleId, long membreId, RegDate dateDebut) {
		this.modeleId = modeleId;
		this.membreId = membreId;
		this.dateDebut = dateDebut;
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

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}
}

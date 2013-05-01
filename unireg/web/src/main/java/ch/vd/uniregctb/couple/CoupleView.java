package ch.vd.uniregctb.couple;

import ch.vd.registre.base.date.RegDate;

public class CoupleView {

	private Long pp1Id;
	private Long pp2Id;
	private boolean marieSeul;
	private boolean nouveauMC = true;
	private Long mcId;
	private RegDate dateDebut;
	private String remarque;

	public Long getPp1Id() {
		return pp1Id;
	}

	public void setPp1Id(Long pp1Id) {
		this.pp1Id = pp1Id;
	}

	public Long getPp2Id() {
		return pp2Id;
	}

	public void setPp2Id(Long pp2Id) {
		this.pp2Id = pp2Id;
	}

	public boolean isMarieSeul() {
		return marieSeul;
	}

	public void setMarieSeul(boolean marieSeul) {
		this.marieSeul = marieSeul;
	}

	public boolean isNouveauMC() {
		return nouveauMC;
	}

	public void setNouveauMC(boolean nouveauMC) {
		this.nouveauMC = nouveauMC;
	}

	public Long getMcId() {
		return mcId;
	}

	public void setMcId(Long mcId) {
		this.mcId = mcId;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}
}

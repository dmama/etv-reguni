package ch.vd.unireg.rapport.view;

import ch.vd.registre.base.date.RegDate;

public class SetPrincipalView {

	private long id;
	private long defuntId;
	private long heritierId;
	private RegDate dateDebut;

	public SetPrincipalView() {
	}

	public SetPrincipalView(long id, long defuntId, long heritierId) {
		this.id = id;
		this.defuntId = defuntId;
		this.heritierId = heritierId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getDefuntId() {
		return defuntId;
	}

	public void setDefuntId(long defuntId) {
		this.defuntId = defuntId;
	}

	public long getHeritierId() {
		return heritierId;
	}

	public void setHeritierId(long heritierId) {
		this.heritierId = heritierId;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}
}

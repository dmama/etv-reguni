package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class MockOrigine implements Origine {

	private String nomLieu;
	private String sigleCanton;
	private Pays pays;

	@Override
	public String getNomLieu() {
		return nomLieu;
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	public void setNomLieu(String nomLieu) {
		this.nomLieu = nomLieu;
	}

	public void setSigleCanton(String sigleCanton) {
		this.sigleCanton = sigleCanton;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}
}

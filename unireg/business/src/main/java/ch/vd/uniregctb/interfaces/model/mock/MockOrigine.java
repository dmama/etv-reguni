package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Origine;

public class MockOrigine implements Origine {

	private String nomLieu;

	@Override
	public String getNomLieu() {
		return nomLieu;
	}

	public void setNomLieu(String nomLieu) {
		this.nomLieu = nomLieu;
	}
}

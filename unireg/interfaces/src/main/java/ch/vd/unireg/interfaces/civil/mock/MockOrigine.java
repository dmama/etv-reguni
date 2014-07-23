package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.unireg.interfaces.civil.data.Origine;

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

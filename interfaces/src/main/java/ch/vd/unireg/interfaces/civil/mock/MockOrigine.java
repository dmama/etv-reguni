package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.unireg.interfaces.civil.data.Origine;

public class MockOrigine implements Origine {

	private final String nomLieu;
	private final String sigleCanton;

	public MockOrigine(String nomLieu, String sigleCanton) {
		this.nomLieu = nomLieu;
		this.sigleCanton = sigleCanton;
	}

	@Override
	public String getNomLieu() {
		return nomLieu;
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}
}

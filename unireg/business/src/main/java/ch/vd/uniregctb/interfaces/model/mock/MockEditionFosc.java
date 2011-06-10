package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEditionFosc implements EditionFosc {

	private int annee;
	private int numero;

	@Override
	public int getAnnee() {
		return annee;
	}

	public void setAnnee(int annee) {
		this.annee = annee;
	}

	@Override
	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}
}

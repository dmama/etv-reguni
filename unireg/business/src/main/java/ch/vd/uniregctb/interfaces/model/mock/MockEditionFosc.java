package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEditionFosc implements EditionFosc {

	private int annee;
	private int numero;
	private RegDate dateParution;

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

	@Override
	public RegDate getDateParution() {
		return dateParution;
	}

	public void setDateParution(RegDate dateParution) {
		this.dateParution = dateParution;
	}
}

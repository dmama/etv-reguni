package ch.vd.uniregctb.entreprise;

import ch.vd.uniregctb.interfaces.model.EditionFosc;

public class EditionFoscView {

	private int annee;
	private int numero;

	public EditionFoscView() {
	}

	public EditionFoscView(EditionFosc edition) {
		this.annee = edition.getAnnee();
		this.numero = edition.getNumero();
	}

	public int getAnnee() {
		return annee;
	}

	public void setAnnee(int annee) {
		this.annee = annee;
	}

	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}
}

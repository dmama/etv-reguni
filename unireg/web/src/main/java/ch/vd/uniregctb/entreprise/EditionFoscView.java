package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EditionFosc;

public class EditionFoscView {

	private int annee;
	private int numero;
	private RegDate dateParution;

	public EditionFoscView() {
	}

	public EditionFoscView(EditionFosc edition) {
		this.annee = edition.getAnnee();
		this.numero = edition.getNumero();
		this.dateParution = edition.getDateParution();
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

	public RegDate getDateParution() {
		return dateParution;
	}

	public void setDateParution(RegDate dateParution) {
		this.dateParution = dateParution;
	}
}

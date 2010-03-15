package ch.vd.uniregctb.evenement.view;

import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;

public class EvenementCivilRegroupeView extends EvenementCivilRegroupe{

	/**
	 *
	 */
	private static final long serialVersionUID = 1822889718034929426L;

	private Long numeroCTB;

	private String nom1;

	private String nom2;

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

}

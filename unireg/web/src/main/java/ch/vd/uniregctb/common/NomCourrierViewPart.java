package ch.vd.uniregctb.common;

import java.util.List;

/**
 * Classe de view destinée à être aggrégée dans une autre vue pour ce qui concerne
 * les nom courrier 1 et nom courrier 2 (en particulier la gestion de l'assignation
 * depuis une liste de chaînes de caractères, voir {@link #setNomCourrier})
 */
public class NomCourrierViewPart {

	private String nomCourrier1;

	private String nomCourrier2;

	public NomCourrierViewPart() {
	}

	public NomCourrierViewPart(String nomCourrier1) {
		this.nomCourrier1 = nomCourrier1;
	}

	public NomCourrierViewPart(List<String> nomCourrier) {
		setNomCourrier(nomCourrier);
	}

	public void setNomCourrier(List<String> nomCourrier) {
		if (nomCourrier != null && nomCourrier.size() > 0) {
			nomCourrier1 = nomCourrier.get(0);
			if (nomCourrier.size() > 1) {
				nomCourrier2 = nomCourrier.get(1);
			}
			else {
				nomCourrier2 = null;
			}
		}
		else {
			nomCourrier1 = null;
			nomCourrier2 = null;
		}
	}

	public String getNomCourrier1() {
		return nomCourrier1;
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier1 = nomCourrier1;
	}

	public String getNomCourrier2() {
		return nomCourrier2;
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier2 = nomCourrier2;
	}


}

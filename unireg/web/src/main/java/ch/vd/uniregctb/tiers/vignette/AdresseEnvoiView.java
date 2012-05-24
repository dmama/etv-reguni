package ch.vd.uniregctb.tiers.vignette;

import ch.vd.uniregctb.adresse.AdresseEnvoi;

public class AdresseEnvoiView {
	private String ligne1;
	private String ligne2;
	private String ligne3;
	private String ligne4;
	private String ligne5;
	private String ligne6;

	public AdresseEnvoiView(AdresseEnvoi adresse) {
		this.ligne1 = adresse.getLigne1();
		this.ligne2 = adresse.getLigne2();
		this.ligne3 = adresse.getLigne3();
		this.ligne4 = adresse.getLigne4();
		this.ligne5 = adresse.getLigne5();
		this.ligne6 = adresse.getLigne6();
	}

	public String getLigne1() {
		return ligne1;
	}

	public String getLigne2() {
		return ligne2;
	}

	public String getLigne3() {
		return ligne3;
	}

	public String getLigne4() {
		return ligne4;
	}

	public String getLigne5() {
		return ligne5;
	}

	public String getLigne6() {
		return ligne6;
	}
}

package ch.vd.uniregctb.registrefoncier;

public class PeriodeFiscaleView {

	private final int annee;
	private final boolean interdite;

	public PeriodeFiscaleView(int annee, boolean interdite) {
		this.annee = annee;
		this.interdite = interdite;
	}

	public int getAnnee() {
		return annee;
	}

	public boolean isInterdite() {
		return interdite;
	}
}

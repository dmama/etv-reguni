package ch.vd.uniregctb.fourreNeutre;

import ch.vd.uniregctb.tiers.Tiers;

public class FourreNeutre {
	private Tiers tiers;
	private int periodeFIscale;


	public FourreNeutre() {
	}

	public FourreNeutre(Tiers tiers, int periodeFIscale) {
		this.tiers = tiers;
		this.periodeFIscale = periodeFIscale;
	}

	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	public int getPeriodeFIscale() {
		return periodeFIscale;
	}

	public void setPeriodeFIscale(int periodeFIscale) {
		this.periodeFIscale = periodeFIscale;
	}
}

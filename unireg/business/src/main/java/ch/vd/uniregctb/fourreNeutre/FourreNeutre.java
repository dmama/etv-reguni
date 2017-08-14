package ch.vd.uniregctb.fourreNeutre;

import ch.vd.uniregctb.tiers.Tiers;

public class FourreNeutre {

	private final Tiers tiers;
	private final int periodeFiscale;

	public FourreNeutre(Tiers tiers, int periodeFiscale) {
		this.tiers = tiers;
		this.periodeFiscale = periodeFiscale;
	}

	public Tiers getTiers() {
		return tiers;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}
}

package ch.vd.uniregctb.fourreNeutre;

import ch.vd.uniregctb.tiers.Tiers;

public class MockFourreNeutreHelper implements FourreNeutreHelper {
	@Override
	public Integer getPremierePeriodePP() {
		return null;
	}

	@Override
	public Integer getPremierePeriodePM() {
		return null;
	}

	@Override
	public Integer getPremierePeriodeIS() {
		return null;
	}

	@Override
	public boolean isTiersAutorisePourFourreNeutre(Tiers tiers) {
		return false;
	}
}

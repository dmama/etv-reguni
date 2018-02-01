package ch.vd.unireg.fourreNeutre;

import ch.vd.unireg.tiers.Tiers;

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

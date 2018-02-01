package ch.vd.uniregctb.etiquette;

import ch.vd.registre.base.date.RegDate;

/**
 * Décalage corrigé d'une date (= décalage simple + correction ultérieure)
 */
public class DecalageAvecCorrection extends Decalage {

	private CorrectionSurDate correction;

	public DecalageAvecCorrection(int decalage, UniteDecalageDate uniteDecalage, CorrectionSurDate correction) {
		super(decalage, uniteDecalage);
		this.correction = correction;
	}

	public DecalageAvecCorrection(DecalageAvecCorrection source) {
		super(source);
		this.correction = source.correction;
	}

	public CorrectionSurDate getCorrection() {
		return correction;
	}

	public void setCorrection(CorrectionSurDate correction) {
		this.correction = correction;
	}

	@Override
	public RegDate apply(RegDate date) {
		final RegDate avantCorrection = super.apply(date);
		return correction.apply(avantCorrection);
	}
}

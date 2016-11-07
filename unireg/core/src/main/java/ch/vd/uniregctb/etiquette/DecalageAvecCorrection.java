package ch.vd.uniregctb.etiquette;

import java.util.function.Function;

import ch.vd.registre.base.date.RegDate;

public class DecalageAvecCorrection implements Function<RegDate, RegDate> {

	private int decalage;
	private UniteDecalageDate uniteDecalage;
	private CorrectionSurDate correction;

	public DecalageAvecCorrection(int decalage, UniteDecalageDate uniteDecalage, CorrectionSurDate correction) {
		this.decalage = decalage;
		this.uniteDecalage = uniteDecalage;
		this.correction = correction;
	}

	public DecalageAvecCorrection(DecalageAvecCorrection source) {
		this.decalage = source.decalage;
		this.uniteDecalage = source.uniteDecalage;
		this.correction = source.correction;
	}

	public int getDecalage() {
		return decalage;
	}

	public void setDecalage(int decalage) {
		this.decalage = decalage;
	}

	public UniteDecalageDate getUniteDecalage() {
		return uniteDecalage;
	}

	public void setUniteDecalage(UniteDecalageDate uniteDecalage) {
		this.uniteDecalage = uniteDecalage;
	}

	public CorrectionSurDate getCorrection() {
		return correction;
	}

	public void setCorrection(CorrectionSurDate correction) {
		this.correction = correction;
	}

	@Override
	public RegDate apply(RegDate date) {
		final RegDate avantCorrection = uniteDecalage.apply(date, decalage);
		return correction.apply(avantCorrection);
	}
}

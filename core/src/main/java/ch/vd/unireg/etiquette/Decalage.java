package ch.vd.unireg.etiquette;

import java.util.function.Function;

import ch.vd.registre.base.date.RegDate;

/**
 * Décalage simple d'une date
 */
public class Decalage implements Function<RegDate, RegDate> {

	private int decalage;
	private UniteDecalageDate uniteDecalage;

	public Decalage(int decalage, UniteDecalageDate uniteDecalage) {
		this.decalage = decalage;
		this.uniteDecalage = uniteDecalage;
	}

	public Decalage(Decalage source) {
		this.decalage = source.decalage;
		this.uniteDecalage = source.uniteDecalage;
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

	@Override
	public RegDate apply(RegDate date) {
		return uniteDecalage.apply(date, decalage);
	}
}

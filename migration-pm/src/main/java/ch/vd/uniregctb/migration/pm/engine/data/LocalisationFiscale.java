package ch.vd.uniregctb.migration.pm.engine.data;

import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class LocalisationFiscale {

	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final int noOfs;

	public LocalisationFiscale(TypeAutoriteFiscale typeAutoriteFiscale, int noOfs) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public int getNoOfs() {
		return noOfs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final LocalisationFiscale that = (LocalisationFiscale) o;
		return noOfs == that.noOfs && typeAutoriteFiscale == that.typeAutoriteFiscale;
	}

	@Override
	public int hashCode() {
		int result = typeAutoriteFiscale != null ? typeAutoriteFiscale.hashCode() : 0;
		result = 31 * result + noOfs;
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s/%d", typeAutoriteFiscale, noOfs);
	}
}

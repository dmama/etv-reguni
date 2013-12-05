package ch.vd.uniregctb.common;

/**
 * Classe qui définit un ordre de tri
 */
public class ParamSorting {

	private final String field;
	private final boolean ascending;

	public ParamSorting(String field, boolean ascending) {
		this.field = field;
		this.ascending = ascending;
	}

	public String getField() {
		return field;
	}

	public boolean isAscending() {
		return ascending;
	}
}

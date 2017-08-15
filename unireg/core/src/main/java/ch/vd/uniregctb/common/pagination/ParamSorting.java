package ch.vd.uniregctb.common.pagination;

import java.io.Serializable;

/**
 * Classe qui définit un ordre de tri
 */
public class ParamSorting implements Serializable {

	private static final long serialVersionUID = -5066653312464423764L;

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

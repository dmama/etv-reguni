package ch.vd.uniregctb.common;

import org.jetbrains.annotations.Nullable;

/**
 * Classe pour definir les paramètres de la pagination
 */
public class ParamPagination {

	private final ParamSorting sorting;

	public ParamPagination(int numeroPage, int taillePage, String champ, boolean sensAscending) {
		this.numeroPage = numeroPage;
		this.taillePage = taillePage;
		this.sorting = new ParamSorting(champ, sensAscending);
	}

	public ParamPagination(String champ, boolean sensAscending) {
		this(1, Integer.MAX_VALUE, champ, sensAscending);
	}

	private final int numeroPage;

	private final int taillePage;

	public int getNumeroPage() {
		return this.numeroPage;
	}

	@SuppressWarnings("UnusedDeclaration")
	public int getTaillePage() {
		return this.taillePage;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getChamp() {
		return sorting.getField();
	}

	public boolean isSensAscending() {
		return sorting.isAscending();
	}

	public ParamSorting getSorting() {
		return this.sorting;
	}

	public int getSqlFirstResult() {
		return (numeroPage - 1) * taillePage;
	}

	public int getSqlMaxResults() {
		return taillePage;
	}

	public static int adjustPage(int numeroPage, int taillePage, int totalCount) {
		return Math.min(numeroPage, (totalCount / taillePage) + 1);
	}

	public static interface CustomOrderByGenerator {
		public boolean supports(String fieldName);

		public String generate(String fieldName, ParamPagination pagination);
	}

	public String buildOrderClause(String tableAlias, @Nullable String defaultField, boolean defaultAsc, @Nullable CustomOrderByGenerator customGenerator) {
		String clauseOrder;
		final String champ = sorting.getField();
		if (champ != null) {
			if (customGenerator != null && customGenerator.supports(champ)) {
				clauseOrder = "order by " + customGenerator.generate(champ, this);
			}
			else if (champ.equals("type")) {
				clauseOrder = " order by " + tableAlias + ".class";
			}
			else {
				clauseOrder = " order by " + tableAlias + "." + champ;
			}

			if (sorting.isAscending()) {
				clauseOrder = clauseOrder + " asc";
			}
			else {
				clauseOrder = clauseOrder + " desc";
			}
		}
		else {
			if (defaultField == null) {
				clauseOrder = " order by " + tableAlias + ".id " + (defaultAsc ? "asc" : "desc");
			}
			else {
				clauseOrder = " order by " + tableAlias + "." + defaultField + " " + (defaultAsc ? "asc" : "desc");
			}
		}

		// [SIFISC-4227] si on ne trie pas sur un index unique, on a des problèmes potentiels avec la pagination
		// donc on ajoute la colonne id dans l'order by comme work-around
		if ((champ != null && !"id".equals(champ)) || defaultField != null) {
			clauseOrder += ", " + tableAlias + ".id asc";
		}

		return clauseOrder;
	}
}

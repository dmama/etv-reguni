package ch.vd.uniregctb.common;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.dbutils.QueryFragment;

/**
 * Classe pour definir les paramètres de la pagination
 */
public class ParamPagination implements Serializable {

	private static final long serialVersionUID = 271854203815541900L;

	private static final Pattern FIELD_ALLOWED_CHARS = Pattern.compile("^[a-zA-Z0-9_.]+$");

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

	public interface CustomOrderByGenerator {
		boolean supports(String fieldName);
		QueryFragment generate(String fieldName, ParamPagination pagination);
	}

	public QueryFragment buildOrderClause(String tableAlias, @Nullable String defaultField, boolean defaultAsc, @Nullable CustomOrderByGenerator customGenerator) {
		QueryFragment clauseOrder = new QueryFragment();
		final String champ = sorting.getField();
		if (champ != null) {
			if (customGenerator != null && customGenerator.supports(champ)) {
				clauseOrder.add("order by ").add(customGenerator.generate(champ, this));
			}
			else {
				// check that the field name does not contain anything except allowed characters
				final Matcher matcher = FIELD_ALLOWED_CHARS.matcher(champ);
				if (!matcher.matches()) {
					throw new IllegalArgumentException("Field name '" + champ + "' not supported as sorting criterion");
				}
				clauseOrder.add("order by " + tableAlias + "." + champ);
			}

			if (sorting.isAscending()) {
				clauseOrder.add(" asc");
			}
			else {
				clauseOrder.add(" desc");
			}
		}
		else {
			if (defaultField == null) {
				clauseOrder.add("order by " + tableAlias + ".id " + (defaultAsc ? "asc" : "desc"));
			}
			else {
				clauseOrder.add("order by " + tableAlias + "." + defaultField + " " + (defaultAsc ? "asc" : "desc"));
			}
		}

		// [SIFISC-4227] si on ne trie pas sur un index unique, on a des problèmes potentiels avec la pagination
		// donc on ajoute la colonne id dans l'order by comme work-around
		if ((champ != null && !"id".equals(champ)) || defaultField != null) {
			clauseOrder.add(", " + tableAlias + ".id asc");
		}

		return clauseOrder;
	}
}

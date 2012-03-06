package ch.vd.uniregctb.common;

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

	public int getTaillePage() {
		return this.taillePage;
	}

	public String getChamp() {
		return sorting.getField();
	}

	public boolean isSensAscending() {
		return sorting.isAscending();
	}

	public ParamSorting getSorting() {
		return this.sorting;
	}

	public static int adjustPage(int numeroPage, int taillePage, int totalCount) {
		return Math.min(numeroPage, (totalCount / taillePage) + 1);
	}
}

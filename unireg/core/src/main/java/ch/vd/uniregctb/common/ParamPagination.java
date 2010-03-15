package ch.vd.uniregctb.common;

/**
 * Classe pour definir les parametres de la pagination
 *
 * @author xcifde
 *
 */
public class ParamPagination {

	public ParamPagination() {
	}

	public ParamPagination(int numeroPage, int taillePage, String champ, boolean sensAscending) {
		this.champ = champ;
		this.numeroPage = numeroPage;
		this.sensAscending = sensAscending;
		this.taillePage = taillePage;
	}

	private int numeroPage;

	private int taillePage;

	private String champ;

	private boolean sensAscending;

	public int getNumeroPage() {
		return numeroPage;
	}

	public void setNumeroPage(int numeroPage) {
		this.numeroPage = numeroPage;
	}

	public int getTaillePage() {
		return taillePage;
	}

	public void setTaillePage(int taillePage) {
		this.taillePage = taillePage;
	}

	public String getChamp() {
		return champ;
	}

	public void setChamp(String champ) {
		this.champ = champ;
	}

	public boolean isSensAscending() {
		return sensAscending;
	}

	public void setSensAscending(boolean sensAscending) {
		this.sensAscending = sensAscending;
	}

}

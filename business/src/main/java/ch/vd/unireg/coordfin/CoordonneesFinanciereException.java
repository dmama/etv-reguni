package ch.vd.unireg.coordfin;

/**
 * Exception dénotant un problème de cohérence des données détectés lors de la manipulation des coordonnées bancaire
 */
public class CoordonneesFinanciereException extends Exception {


	private static final long serialVersionUID = -8770387445727672303L;

	public CoordonneesFinanciereException(Throwable e) {
		super(e);
	}

	public CoordonneesFinanciereException(String string) {
		super(string);
	}

	public CoordonneesFinanciereException(String string, Throwable e) {
		super(string, e);
	}
}

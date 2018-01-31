package ch.vd.uniregctb.metier.assujettissement;

/**
 * Exception dénotant un problème de cohérence des données détectés lors de la détermination de l'assujettissement d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AssujettissementException extends Exception {

	private static final long serialVersionUID = -5347701763768846767L;

	public AssujettissementException(Throwable e) {
		super(e);
	}

	public AssujettissementException(String string) {
		super(string);
	}

	public AssujettissementException(String string, Throwable e) {
		super(string, e);
	}
}

package ch.vd.uniregctb.evenement.ide;

import java.util.List;

import ch.vd.registre.base.utils.Pair;

/**
 * @author Raphaël Marmier, 2016-09-06, <raphael.marmier@vd.ch>
 */
public class AnnonceIDEValidationException extends ServiceIDEException {

	private static final long serialVersionUID = -4534939716072502301L;

	List<Pair<String, String>> erreurs;

	public AnnonceIDEValidationException() {
		super();
	}

	/**
	 * @param message message décrivant le problème.
	 */
	public AnnonceIDEValidationException(String message, List<Pair<String, String>> erreurs) {
		super(message);
		this.erreurs = erreurs;
	}

	public List<Pair<String, String>> getErreurs() {
		return erreurs;
	}
}

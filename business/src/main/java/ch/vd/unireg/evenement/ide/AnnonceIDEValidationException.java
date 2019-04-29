package ch.vd.unireg.evenement.ide;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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

package ch.vd.uniregctb.iban;

/**
 * Exception lancée lorsque l'iban n'est pas en majuscule.
 * @author xsibnm
 *
 */
public class IbanUpperCaseException extends IbanValidationException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1922076219533456L;

	/**
	 * Constructeur par défaut.
	 */
	public IbanUpperCaseException() {
		super("L'IBAN contient une ou plusieurs lettres en minuscule");
	}
}
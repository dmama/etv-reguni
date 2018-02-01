package ch.vd.unireg.editique;

import ch.vd.technical.esb.ErrorType;

/**
 * Interface implémentée par un résultat d'impression en erreur
 */
public interface EditiqueResultatErreur extends EditiqueResultatRecu {

	/**
	 * @return le message en cas d'erreur, sinon <codeb>null</code>.
	 */
	String getErrorMessage();

	/**
	 * @return le type de l'erreur
	 */
	ErrorType getErrorType();

	/**
	 * D'après la documentation éditique, ce code peut être
	 * <ul>
	 *     <li>404&nbsp;: document non trouvé</li>
	 *     <li>412&nbsp;: paramètre invalide</li>
	 *     <li>500&nbsp;: erreur interne au service</li>
	 *     <li>504&nbsp;: timeout (<b>extension unireg</b>)</li>
	 * </ul>
	 * @return le code de l'erreur
	 */
	Integer getErrorCode();
}

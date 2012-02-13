package ch.vd.uniregctb.evenement.civil;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Interface de génération découplée d'erreurs lors du traitement des événements civils
 */
public abstract class EvenementCivilErreurFactory<T extends EvenementCivilErreur> {

	/**
	 * Crée une erreur (de type {@link TypeEvenementErreur#ERROR}) avec le message et la callstack définis par l'exception passée en paramètre
	 * @param e exception dont on doit extraire la callstack et le message
	 * @return un objet erreur qui contient tout ça
	 */
	public T createErreur(Exception e) {
		if (e == null) {
			throw new NullPointerException("Exception cannot be null!");
		}
		return createErreur(StringUtils.EMPTY, e, TypeEvenementErreur.ERROR);
	}

	/**
	 * Crée une erreur (de type {@link TypeEvenementErreur#ERROR}) avec le message passé en paramètre
	 * @param message message d'erreur
	 * @return un objet erreur qui contient tout ça
	 */
	public T createErreur(String message) {
		if (StringUtils.isBlank(message)) {
			throw new IllegalArgumentException("Le message d'erreur ne peut pas être vide!");
		}
		return createErreur(message, null, TypeEvenementErreur.ERROR);
	}

	/**
	 * Crée une erreur (ou un warning, selon le type donné) avec le message donné
	 * @param message message à inclure dans l'erreur
	 * @param type classification de l'erreur
	 * @return un objet erreur qui contient tout ça
	 */
	public T createErreur(String message, TypeEvenementErreur type) {
		return createErreur(message, null, type);
	}

	/**
	 * Réelle implémentation centralisée de la création d'une erreur avec tous les paramètres donnés
	 * @param message message d'erreur
	 * @param e exception (optionnelle) qui a donné lieu à l'erreur elle-même
	 * @param type type d'erreur
	 * @return un objet erreur qui contient tout ça
	 */
	protected abstract T createErreur(String message, @Nullable Exception e, TypeEvenementErreur type);

	/**
	 * Construction de la véritable valeur à associée au champ "message" de l'erreur, en fonction de la présence ou non d'une exception
	 * @param message message de base
	 * @param e exception (optionnelle) qui a donné lieu à l'erreur
	 * @return une chaîne de caractères à utiliser dans le message de l'erreur
	 */
	protected static String buildActualMessage(String message, @Nullable Exception e) {
		message = StringUtils.trimToNull(message);
		if (e == null) {
			return message;
		}

		final String exceptionInfo = StringUtils.isBlank(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
		if (message == null) {
			return exceptionInfo;
		}

		return String.format("%s - %s", message, exceptionInfo);
	}

	/**
	 * Extraction de la callstack d'une exception
	 * @param e exception
	 * @return la callstack, ou <code>null</code> si l'exception était nulle
	 */
	protected static String extractCallstack(@Nullable Exception e) {
		return e != null ? ExceptionUtils.extractCallStack(e) : null;
	}
}

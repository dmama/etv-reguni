package ch.vd.uniregctb.utils;

import javax.servlet.http.HttpSession;
import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

/**
 * Quelques méthodes/constantes utilitaires autour de la session HTTP
 */
public abstract class HttpSessionUtils {

	/**
	 * Récupère une valeur depuis les données de la session HTTP
	 * @param session la session en question
	 * @param name le nom de l'attribut
	 * @param clazz la classe attendue pour la valeur retournée
	 * @param defaultValue la valeur à utiliser par défaut pour initialiser la session quand l'attribut a une valeur vide
	 * @param <T> le type de la valeur attendue
	 * @return la valeur présente en session pour l'attribut nommé donné
	 */
	@Nullable
	public static <T extends Serializable> T getFromSession(HttpSession session, String name, Class<T> clazz, @Nullable T defaultValue) {
		final Object value = session.getAttribute(name);
		if (value == null) {
			if (defaultValue != null) {
				addToSession(session, name, defaultValue);
			}
			return defaultValue;
		}

		if (!clazz.isAssignableFrom(value.getClass())) {
			throw new ClassCastException(String.format("Session's attribute '%s' is of wrong class (%s where %s - or some subclass of it - is expected)",
			                                           name,
			                                           value.getClass().getName(),
			                                           clazz.getName()));
		}

		//noinspection unchecked
		return (T) value;
	}

	/**
	 * Récupère une valeur depuis les données de la session HTTP, en prenant en priorité la valeur "forcée", si une telle valeur est fournie
	 * @param session la session en question
	 * @param name le nom de l'attribut
	 * @param clazz la classe attendue pour la valeur retournée
	 * @param defaultValue la valeur à utiliser par défaut pour initialiser la session quand l'attribut a une valeur vide
	 * @param forcedValue si non-nulle, la valeur à prendre en compte dans la session en écrasant si nécessaire la valeur présente
	 * @param <T> le type de la valeur attendue
	 * @return la valeur présente en session pour l'attribut nommé donné
	 */
	@Nullable
	public static <T extends Serializable> T getFromSession(HttpSession session, String name, Class<T> clazz, @Nullable T defaultValue, @Nullable T forcedValue) {
		if (forcedValue != null) {
			addToSession(session, name, forcedValue);
			return forcedValue;
		}
		return getFromSession(session, name, clazz, defaultValue);
	}

	/**
	 * Ajoute une valeur à la session HTTP
	 * @param session la session en question
	 * @param name le nom de l'attribut
	 * @param value la valeur à stocker (si <code>null</code>, l'attribut sera effacé de la session)
	 */
	public static void addToSession(HttpSession session, String name, @Nullable Serializable value) {
		if (value != null) {
			session.setAttribute(name, value);
		}
		else {
			session.removeAttribute(name);
		}
	}
}

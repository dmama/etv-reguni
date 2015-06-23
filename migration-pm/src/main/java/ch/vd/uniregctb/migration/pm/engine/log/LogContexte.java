package ch.vd.uniregctb.migration.pm.engine.log;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.log.LoggedElement;

/**
 * Un "contexte" de résolution des messages, éléments par défaut des différents types de {@link LoggedElement}
 * qui varient selon l'avancement de la migration d'un graphe et qui sont insérés dans les messages loggués
 * <p/>
 * <b>Important :</b> Cette classe n'est pas <i>thread-safe</i>
 */
public final class LogContexte {

	/**
	 * Les valeurs connues
	 */
	private final Map<Class<?>, LoggedElement> map = new HashMap<>();

	/**
	 * Assigne une nouvelle valeur
	 * @param clazz classe de l'élément à assigner
	 * @param value valeur à assigner à cet élément
	 * @param <T> type de l'élément à assigner
	 */
	public <T extends LoggedElement> void setContextValue(Class<T> clazz, T value) {
		map.put(clazz, value);
	}

	/**
	 * Efface une valeur existante
	 * @param clazz classe de l'élément à effacer
	 * @param <T> type de l'élément à assigner
	 */
	public <T extends LoggedElement> void resetContextValue(Class<T> clazz) {
		map.remove(clazz);
	}

	/**
	 * Récupère une valeur du contexte
	 * @param clazz classe de l'élément à récupérer
	 * @param <T> type de l'élément à assigner
	 */
	public <T extends LoggedElement> T getContextValue(Class<T> clazz) {
		//noinspection unchecked
		return (T) map.get(clazz);
	}
}

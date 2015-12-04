package ch.vd.uniregctb.migration.pm.engine.log;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

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
	 * Les piles d'éléments pour chaque classe (support du push/pop)
	 */
	private final Map<Class<?>, Stack<LoggedElement>> stacks = new HashMap<>();

	/**
	 * Assigne une nouvelle valeur
	 * @param clazz classe de l'élément à assigner
	 * @param value valeur à assigner à cet élément
	 * @param <T> type de l'élément à assigner
	 */
	public <T extends LoggedElement> void pushContextValue(Class<T> clazz, T value) {
		final LoggedElement old = map.put(clazz, value);
		enstackValue(clazz, old);
	}

	/**
	 * Efface une valeur existante
	 * @param clazz classe de l'élément à effacer
	 * @param <T> type de l'élément à assigner
	 * @throws NoSuchElementException s'il n'y a jamais eu de {@link #pushContextValue(Class, LoggedElement)} avec cette même classe
	 * @throws java.util.EmptyStackException si le pop est de trop par rapport aux pushs effectués
	 */
	public <T extends LoggedElement> void popContextValue(Class<T> clazz) {
		final LoggedElement previous = popValue(clazz);
		if (previous == null) {
			map.remove(clazz);
		}
		else {
			map.put(clazz, previous);
		}
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

	private <T extends LoggedElement> void enstackValue(Class<T> clazz, LoggedElement value) {
		final Stack<LoggedElement> stack = stacks.computeIfAbsent(clazz, c -> new Stack<>());
		stack.push(value);
	}

	private <T extends LoggedElement> LoggedElement popValue(Class<T> clazz) {
		final Stack<LoggedElement> stack = stacks.get(clazz);
		if (stack == null) {
			// pop sans push initial ???
			throw new NoSuchElementException("Utilisation de pop sans push préalable!");
		}

		return stack.pop();
	}
}

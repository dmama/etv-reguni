package ch.vd.unireg.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe utilitaire qui permet de gérer une donnée en {@link ThreadLocal} sur plusieurs niveaux, par exemple
 * pour être en mesure d'en suspendre l'effet pendant qu'une transaction est suspendue (pour faire place à une
 * autre en {@link org.springframework.transaction.annotation.Propagation#REQUIRES_NEW})
 * @param <T> type de l'élément contenu dans le {@link ThreadLocal} équivalent
 */
public class StackedThreadLocal<T> {

	/**
	 * Un objet qui sera utilisé dans les stacks comme remplaçant de "null", car cette valeur n'est pas acceptée dans les ArrayDeque...
	 */
	private static final Object NULL_REPLACEMENT = new Object();

	/**
	 * Un objet qui sera utilisé dans les stacks comme indication de "non-initialisé"
	 */
	private static final Object NOT_INITIALIZED = new Object();

	/**
	 * {@link ThreadLocal} à qui est déléguée la partie "valeur différente pour chaque threads"
	 */
	private final ThreadLocal<Deque<Object>> stacks = ThreadLocal.withInitial(this::newStack);

	/**
	 * Récupérateur de nouvel élément
	 */
	private final Supplier<? extends T> supplier;

	/**
	 * Constructeur
	 * @param supplier fournisseur de nouveaux éléments individuels
	 */
	public StackedThreadLocal(Supplier<? extends T> supplier) {
		this.supplier = supplier;
	}

	/**
	 * Constructeur par défaut, qui initialise la donnée du ThreadLocal équivalent à <code>null</code>
	 * (et suppose plus ou moins l'utilisation de la méthode {@link #set})
	 */
	public StackedThreadLocal() {
		this(() -> null);
	}

	/**
	 * Construction d'une nouvelle stack pour le thread courant
	 * @return la nouvelle stack
	 */
	private Deque<Object> newStack() {
		final Deque<Object> stack = new ArrayDeque<>();
		_push(stack, NOT_INITIALIZED);
		return stack;
	}

	/**
	 * @return valeur renvoyée par un nouvel appel au fournisseur officiel de valeurs
	 */
	private T newElement() {
		return supplier.get();
	}

	/**
	 * @param element élément non-encodée (donc potentiellement <code>null</code>)
	 * @return élément encodé (= jamais <code>null</code>)
	 */
	@NotNull
	private Object encodeElement(@Nullable T element) {
		return element != null ? element : NULL_REPLACEMENT;
	}

	/**
	 * @param element élément encodé (= jamais <code>null</code>)
	 * @return élément décodé (donc potentiellement <code>null</code>)
	 */
	@Nullable
	private T decodeElement(@NotNull Object element) {
		//noinspection unchecked
		return element == NULL_REPLACEMENT ? null : (T) element;
	}

	/**
	 * Accès à la stack du thread courant
	 * @return la stack du thread courant, éventuellement instanciée à la demande
	 */
	@NotNull
	private Deque<Object> getStack() {
		return stacks.get();
	}

	/**
	 * Mise de côté du contenu actuel et démarrage d'un nouveau contexte
	 */
	public void pushState() {
		_push(getStack(), NOT_INITIALIZED);
	}

	/**
	 * Méthode interne d'empilement d'une nouvelle valeur
	 * @param stack stack sur laquelle on veut poser la nouvelle valeur
	 * @param value nouvelle valeur
	 * @param <T> type des valeurs placées dans la stack
	 */
	private static <T> void _push(Deque<T> stack, @NotNull T value) {
		stack.push(value);
	}

	/**
	 * Récupération du contenu mis de côté au préalable
	 * @throws NoSuchElementException si aucun appel à {@link #pushState()} n'a été fait au préalable
	 */
	public void popState() {
		_pop(getStack(), false);
	}

	/**
	 * Méthode interne d'oubli de la dernière valeur de la stack fournie
	 * @param stack stack dont on veut dépiler (et oublier) l'élément courant
	 * @param lastStatePoppable <code>true</code> si cette opération est autorisée à enlever le dernier élément de la stack, <code>false</code> sinon
	 * @throws NoSuchElementException s'il n'y a déjà plus d'élément dans la stack, ou s'il n'en reste qu'un mais qu'il n'est pas permis de l'enlever
	 */
	private static void _pop(Deque<?> stack, boolean lastStatePoppable) {
		if (stack.isEmpty() || (stack.size() == 1 && !lastStatePoppable)) {
			throw new NoSuchElementException("Cannot pop last state!!");
		}
		stack.pop();
	}

	/**
	 * Accès au contenu courant
	 * @return le contenu du {@link ThreadLocal}
	 */
	public T get() {
		final Deque<Object> stack = getStack();
		final Object current = stack.peek();
		if (current == NOT_INITIALIZED) {
			final T newElement = newElement();
			_set(stack, encodeElement(newElement));
			return newElement;
		}
		return decodeElement(current);
	}

	/**
	 * Assignatione explicite du contenu courant sans passer par le fournisseur officiel
	 * @param value la valeur explicite à assigner
	 */
	public void set(T value) {
		_set(encodeElement(value));
	}

	/**
	 * Méthode interne d'assignation de la valeur courante
	 * @param encoded valeur courante à assigner (déjà encodée par rapport à la valeur nulle)
	 */
	private void _set(Object encoded) {
		_set(getStack(), encoded);
	}

	/**
	 * Méthode interne d'assignation d'une valeur comme dernière valeur (= remplacement) de la stack fournie
	 * @param stack stack cible
	 * @param encoded valeur courante à assigner (déjà encodée par rapport à la valeur nulle)
	 */
	private static void _set(Deque<Object> stack, Object encoded) {
		_pop(stack, true);
		_push(stack, encoded);
	}

	/**
	 * Ré-initialisation explicite du contenu afin que le prochain appel à {@link #get()} fournisse une nouvelle valeur
	 * générée par le fournisseur officiel
	 */
	public void reset() {
		_set(NOT_INITIALIZED);
	}
}

package ch.vd.uniregctb.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

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

	private final ThreadLocal<Deque<Object>> stacks = ThreadLocal.withInitial(this::newStack);
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
		stack.push(newElement());
		return stack;
	}

	private Object newElement() {
		final T fromSupplier = supplier.get();
		return encodeElement(fromSupplier);
	}

	private Object encodeElement(T element) {
		return element != null ? element : NULL_REPLACEMENT;
	}

	private T decodeElement(Object element) {
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
	 * Mise de côté du contenu actuel et initialisation d'un nouveau contexte
	 */
	public void pushState() {
		push(getStack(), newElement());
	}

	private static <T> void push(Deque<T> stack, T value) {
		stack.push(value);
	}

	/**
	 * Récupération du contenu mis de côté au préalable
	 */
	public void popState() {
		pop(getStack(), false);
	}

	private static <T> void pop(Deque<T> stack, boolean lastStatePoppable) {
		if (stack.isEmpty() || (stack.size() == 1 && !lastStatePoppable)) {
			throw new IllegalStateException("Cannot pop last state!!");
		}
		stack.pop();
	}

	/**
	 * Accès au contenu courant
	 * @return le contenu du {@link ThreadLocal}
	 */
	public T get() {
		return decodeElement(getStack().peek());
	}

	/**
	 * Assignatione explicite du contenu courant sans passer par le fournisseur officiel
	 * @param value la valeur explicite à assigner
	 */
	public void set(T value) {
		final Deque<Object> stack = getStack();
		pop(stack, true);
		push(stack, encodeElement(value));
	}
}

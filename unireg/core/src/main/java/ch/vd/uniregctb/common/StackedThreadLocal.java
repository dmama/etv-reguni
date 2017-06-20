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

	private final ThreadLocal<Deque<T>> stacks = ThreadLocal.withInitial(this::newStack);
	private final Supplier<? extends T> supplier;

	/**
	 * Constructeur
	 * @param supplier fournisseur de nouveaux éléments individuels
	 */
	public StackedThreadLocal(Supplier<? extends T> supplier) {
		this.supplier = supplier;
	}

	/**
	 * Construction d'une nouvelle stack pour le thread courant
	 * @return la nouvelle stack
	 */
	private Deque<T> newStack() {
		final Deque<T> stack = new ArrayDeque<>();
		stack.push(supplier.get());
		return stack;
	}

	/**
	 * Accès à la stack du thread courant
	 * @return la stack du thread courant, éventuellement instanciée à la demande
	 */
	@NotNull
	private Deque<T> getStack() {
		return stacks.get();
	}

	/**
	 * Mise de côté du contenu actuel et initialisation d'un nouveau contexte
	 */
	public void pushState() {
		push(getStack(), supplier.get());
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
		return getStack().peek();
	}

	/**
	 * Assignatione explicite du contenu courant sans passer par le fournisseur officiel
	 * @param value la valeur explicite à assigner
	 */
	public void set(T value) {
		final Deque<T> stack = getStack();
		pop(stack, true);
		push(stack, value);
	}
}

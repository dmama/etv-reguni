package ch.vd.uniregctb.common;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Classe qui permet d'ouvrir/fermer plusieurs switches en même temps en maintenant une pile de leurs états précédents
 * si nécessaire
 */
public class MultipleSwitch {

	private final Switchable[] switches;
	private final ThreadLocal<Deque<boolean[]>> stateStack = ThreadLocal.withInitial(ArrayDeque::new);

	public MultipleSwitch(Switchable... switches) {
		if (switches == null) {
			throw new NullPointerException("switches");
		}
		for (int i = 0 ; i < switches.length; ++ i) {
			if (switches[i] == null) {
				throw new NullPointerException("switches[" + i + "]");
			}
		}
		this.switches = switches;
	}

	/**
	 * Sauvegarde les états courants de tous les switches concernés
	 */
	public void pushState() {
		final boolean[] state = new boolean[switches.length];
		int index = 0;
		for (Switchable switchable : switches) {
			state[index ++] = switchable.isEnabled();
		}
		stateStack.get().push(state);
	}

	/**
	 * Restaure la dernière sauvegarde des états des switches
	 * @throws java.util.NoSuchElementException s'il n'y a pas de sauvegarde disponible
	 */
	public void popState() {
		final boolean[] state = stateStack.get().pop();
		int index = 0;
		for (Switchable switchable : switches) {
			switchable.setEnabled(state[index ++]);
		}
	}

	/**
	 * Place tous les switches dans l'état demandé
	 * @param enabled nouvel état
	 */
	public void setEnabled(boolean enabled) {
		for (Switchable switchable : switches) {
			switchable.setEnabled(enabled);
		}
	}
}

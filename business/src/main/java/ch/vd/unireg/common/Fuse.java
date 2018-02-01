package ch.vd.unireg.common;

/**
 * Classe qui permet de déclarer un "fusible", une transition qui ne peut se faire
 * que dans un sens et une seule fois
 */
public final class Fuse {

	/**
	 * Etat du fusible : à la création, le courant passe, état 1
	 */
	private volatile boolean blown = false;

	/**
	 * @return <code>true</code> si le fusible est passé dans son état 2
	 */
	public boolean isBlown() {
		return blown;
	}

	/**
	 * @return <code>true</code> si le fusible est encore dans l'état 1
	 */
	public boolean isNotBlown() {
		return !blown;
	}

	/**
	 * Le courant ne passe plus, état 2 (final). La méthode {@link #notifyAll()} est appelée si l'état est modifié.
	 */
	public void blow() {
		if (!blown) {
			doBlow();
		}
	}

	private synchronized void doBlow() {
		if (!blown) {
			blown = true;
			notifyAll();
		}
	}
}

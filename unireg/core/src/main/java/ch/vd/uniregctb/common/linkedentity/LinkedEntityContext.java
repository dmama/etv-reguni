package ch.vd.uniregctb.common.linkedentity;

/**
 * Les informations du context dans lequel on demande la liste des entités liées.
 */
public class LinkedEntityContext {

	private final LinkedEntityPhase phase;

	public LinkedEntityContext(LinkedEntityPhase phase) {
		this.phase = phase;
	}

	public LinkedEntityPhase getPhase() {
		return phase;
	}
}

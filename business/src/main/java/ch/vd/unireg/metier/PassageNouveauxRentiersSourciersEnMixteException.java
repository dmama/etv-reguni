package ch.vd.unireg.metier;

import ch.vd.unireg.tiers.Contribuable;

public class PassageNouveauxRentiersSourciersEnMixteException extends Exception {
	private final Contribuable ctb;
	private final PassageNouveauxRentiersSourciersEnMixteResults.ErreurType type;

	public PassageNouveauxRentiersSourciersEnMixteException(Contribuable ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType type) {
		this.ctb = ctb;
		this.type = type;
	}

	public PassageNouveauxRentiersSourciersEnMixteException(Contribuable ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType type, Throwable cause) {
		super(cause);
		this.ctb = ctb;
		this.type = type;
	}

	public PassageNouveauxRentiersSourciersEnMixteException(Contribuable ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType type, String message) {
		super(message);
		this.ctb = ctb;
		this.type = type;
	}

	public Contribuable getContribuable() {
		return ctb;
	}

	public PassageNouveauxRentiersSourciersEnMixteResults.ErreurType getType() {
		return type;
	}
}

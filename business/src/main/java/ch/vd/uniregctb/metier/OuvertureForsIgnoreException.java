package ch.vd.uniregctb.metier;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Classe utilisée pour signaler un contribuable ignoré
 */
class OuvertureForsIgnoreException extends Exception {

	private static final long serialVersionUID = -4542845243959539157L;

	private final PersonnePhysique pp;
	private final OuvertureForsResults.IgnoreType raison;
	private final String details;

	public OuvertureForsIgnoreException(PersonnePhysique pp, OuvertureForsResults.IgnoreType raison, @Nullable String details) {
		this.pp = pp;
		this.raison = raison;
		this.details = details;
	}

	public PersonnePhysique getPersonnePhysique() {
		return pp;
	}

	public OuvertureForsResults.IgnoreType getRaison() {
		return raison;
	}

	public String getDetails() {
		return details;
	}
}

package ch.vd.uniregctb.tiers;

import ch.vd.uniregctb.common.ObjectNotFoundException;

/**
 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver un individu.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IndividuNotFoundException extends ObjectNotFoundException {

	private static final long serialVersionUID = -736783238495398724L;

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'individu rattaché à un habitant.
	 */
	public IndividuNotFoundException(PersonnePhysique personne) {
		super(buildMessage(personne));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'individu à partir de son numéro.
	 */
	public IndividuNotFoundException(long noIndividu) {
		super(buildMessage(noIndividu));
	}

	private static String buildMessage(PersonnePhysique personne) {
		return String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d",
		                     personne.getNumeroIndividu(),
		                     personne.getNumero());
	}

	private static String buildMessage(long noIndividu) {
		return String.format("Impossible de trouver l'individu n°%d", noIndividu);
	}
}

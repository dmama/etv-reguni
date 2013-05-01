package ch.vd.uniregctb.tiers;

/**
 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver un individu.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IndividuNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -2792170675610539766L;

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
		final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne.getNumeroIndividu(),
				personne.getNumero());
		return message;
	}

	private static String buildMessage(long noIndividu) {
		final String message = String.format("Impossible de trouver l'individu n°%d", noIndividu);
		return message;
	}
}

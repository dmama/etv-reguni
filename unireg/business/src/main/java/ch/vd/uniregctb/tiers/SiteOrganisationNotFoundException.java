package ch.vd.uniregctb.tiers;

import ch.vd.uniregctb.common.ObjectNotFoundException;

/**
 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver une organisation.
 */
public class SiteOrganisationNotFoundException extends ObjectNotFoundException {

	private static final long serialVersionUID = 1416186084348145156L;

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver le site rattachée à un établissement.
	 */
	public SiteOrganisationNotFoundException(Etablissement etb) {
		super(buildMessage(etb));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver un site à partir de son numéro
	 */
	public SiteOrganisationNotFoundException(long noSite) {
		super(buildMessage(noSite));
	}

	private static String buildMessage(Etablissement etb) {
		return String.format("Impossible de trouver le site n°%d pour l'établissment n°%d", etb.getNumeroEtablissement(), etb.getNumero());
	}

	private static String buildMessage(long noOrganisation) {
		return String.format("Impossible de trouver le site n°%d", noOrganisation);
	}
}

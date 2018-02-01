package ch.vd.unireg.tiers;

import ch.vd.unireg.common.ObjectNotFoundException;

/**
 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver une organisation.
 */
public class OrganisationNotFoundException extends ObjectNotFoundException {

	private static final long serialVersionUID = 6627151567392911236L;

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'organisation rattachée à une entreprise.
	 */
	public OrganisationNotFoundException(Entreprise entreprise) {
		super(buildMessage(entreprise));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'organisation rattachée à un établissement.
	 */
	public OrganisationNotFoundException(Etablissement etablissement) {
		super(buildMessage(etablissement));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'organisation à partir de son numéro
	 */
	public OrganisationNotFoundException(long noOrganisation) {
		super(buildMessage(noOrganisation));
	}

	private static String buildMessage(Entreprise entreprise) {
		return String.format("Impossible de trouver l'organisation n°%d pour l'entreprise n°%d", entreprise.getNumeroEntreprise(), entreprise.getNumero());
	}

	private static String buildMessage(Etablissement etablissement) {
		return String.format("Impossible de trouver l'organisation n°%d pour l'établissement n°%d", etablissement.getNumeroEtablissement(), etablissement.getNumero());
	}

	private static String buildMessage(long noOrganisation) {
		return String.format("Impossible de trouver l'organisation n°%d", noOrganisation);
	}
}

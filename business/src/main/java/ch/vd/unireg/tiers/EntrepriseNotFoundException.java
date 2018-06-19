package ch.vd.unireg.tiers;

import ch.vd.unireg.common.ObjectNotFoundException;

/**
 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver une entreprise.
 */
public class EntrepriseNotFoundException extends ObjectNotFoundException {

	private static final long serialVersionUID = 6627151567392911236L;

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'entreprise rattachée à une entreprise.
	 */
	public EntrepriseNotFoundException(Entreprise entreprise) {
		super(buildMessage(entreprise));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'entreprise rattachée à un établissement.
	 */
	public EntrepriseNotFoundException(Etablissement etablissement) {
		super(buildMessage(etablissement));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'entreprise à partir de son numéro
	 */
	public EntrepriseNotFoundException(long noEntrepriseCivile) {
		super(buildMessage(noEntrepriseCivile));
	}

	private static String buildMessage(Entreprise entreprise) {
		return String.format("Impossible de trouver l'entreprise n°%d pour l'entreprise n°%d", entreprise.getNumeroEntreprise(), entreprise.getNumero());
	}

	private static String buildMessage(Etablissement etablissement) {
		return String.format("Impossible de trouver l'entreprise n°%d pour l'établissement n°%d", etablissement.getNumeroEtablissement(), etablissement.getNumero());
	}

	private static String buildMessage(long noEntrepriseCivile) {
		return String.format("Impossible de trouver l'entreprise n°%d", noEntrepriseCivile);
	}
}

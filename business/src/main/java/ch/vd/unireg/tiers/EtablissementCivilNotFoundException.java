package ch.vd.unireg.tiers;

import ch.vd.unireg.common.ObjectNotFoundException;

/**
 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver un établissement civil.
 */
public class EtablissementCivilNotFoundException extends ObjectNotFoundException {

	private static final long serialVersionUID = 1416186084348145156L;

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver l'établissement civil rattachée à un établissement.
	 */
	public EtablissementCivilNotFoundException(Etablissement etb) {
		super(buildMessage(etb));
	}

	/**
	 * Exception spécialisée pour signaler proprement les cas où il n'est pas possible de trouver un établissement civil à partir de son numéro
	 */
	public EtablissementCivilNotFoundException(long noEtablissementCivil) {
		super(buildMessage(noEtablissementCivil));
	}

	private static String buildMessage(Etablissement etb) {
		return String.format("Impossible de trouver l'établissement civil n°%d pour l'établissment n°%d", etb.getNumeroEtablissement(), etb.getNumero());
	}

	private static String buildMessage(long noEtablissementCivil) {
		return String.format("Impossible de trouver l'établissement civil n°%d", noEtablissementCivil);
	}
}

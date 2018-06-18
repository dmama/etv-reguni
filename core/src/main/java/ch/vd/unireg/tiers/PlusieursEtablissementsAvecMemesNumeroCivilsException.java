package ch.vd.unireg.tiers;

import java.util.Arrays;

/**
 * Exception lancée par le TiersDAO lorsque l'on cherche un établissement par numéro
 * d'établissement civil et que l'on en trouve plusieurs (tous non-annulés, bien-sûr)
 */
public class PlusieursEtablissementsAvecMemesNumeroCivilsException extends RuntimeException {

	private final long numeroEtablissementCivil;
	private final long[] noEtablissements;

	/**
	 * @param numeroEtablissementCivil numéro de l'établissement civil recherché
	 * @param noEtablissements tableau contenant les numéros de contribuables des établissements trouvés
	 */
	public PlusieursEtablissementsAvecMemesNumeroCivilsException(long numeroEtablissementCivil, long[] noEtablissements) {
		this.numeroEtablissementCivil = numeroEtablissementCivil;
		this.noEtablissements = noEtablissements;
	}

	@Override
	public String getMessage() {
		return String.format("Plusieurs établissements non-annulés partagent le même numéro d'établissement civil %d (%s)", numeroEtablissementCivil, Arrays.toString(noEtablissements));
	}
}

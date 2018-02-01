package ch.vd.unireg.tiers;

import java.util.Arrays;

/**
 * Exception lancée par le TiersDAO lorsque l'on cherche un établissement par numéro
 * de site et que l'on en trouve plusieurs (tous non-annulés, bien-sûr)
 */
public class PlusieursEtablissementsAvecMemeNumeroSitesException extends RuntimeException {

	private final long numeroSite;
	private final long[] noEtablissements;

	/**
	 * @param numeroSite numéro du site recherché
	 * @param noEtablissements tableau contenant les numéros de contribuables des établissements trouvés
	 */
	public PlusieursEtablissementsAvecMemeNumeroSitesException(long numeroSite, long[] noEtablissements) {
		this.numeroSite = numeroSite;
		this.noEtablissements = noEtablissements;
	}

	@Override
	public String getMessage() {
		return String.format("Plusieurs établissements non-annulés partagent le même numéro de site %d (%s)", numeroSite, Arrays.toString(noEtablissements));
	}
}

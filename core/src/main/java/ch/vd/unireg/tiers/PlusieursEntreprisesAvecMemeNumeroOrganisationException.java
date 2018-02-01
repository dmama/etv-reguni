package ch.vd.unireg.tiers;

import java.util.Arrays;

/**
 * Exception lancée par le TiersDAO lorsque l'on cherche une entreprise par numéro
 * d'organisation et que l'on en trouve plusieurs (toutes non-annulées, bien-sûr)
 */
public class PlusieursEntreprisesAvecMemeNumeroOrganisationException extends RuntimeException {

	private final long numeroOrganisation;
	private final long[] noEntreprises;

	/**
	 * @param numeroOrganisation numéro de l'organisation recherchée
	 * @param noEntreprises tableau contenant les numéros de contribuables des entreprises trouvées
	 */
	public PlusieursEntreprisesAvecMemeNumeroOrganisationException(long numeroOrganisation, long[] noEntreprises) {
		this.numeroOrganisation = numeroOrganisation;
		this.noEntreprises = noEntreprises;
		Arrays.sort(this.noEntreprises);
	}

	@Override
	public String getMessage() {
		return String.format("Plusieurs entreprises non-annulées partagent le même numéro d'organisation %d: (%s)", numeroOrganisation, Arrays.toString(noEntreprises));
	}
}

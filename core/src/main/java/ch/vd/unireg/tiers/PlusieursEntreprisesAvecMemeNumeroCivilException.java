package ch.vd.unireg.tiers;

import java.util.Arrays;

/**
 * Exception lancée par le TiersDAO lorsque l'on cherche une entreprise par numéro
 * d'entreprise civile et que l'on en trouve plusieurs (toutes non-annulées, bien-sûr)
 */
public class PlusieursEntreprisesAvecMemeNumeroCivilException extends RuntimeException {

	private final long numeroEntrepriseCivile;
	private final long[] noEntreprises;

	/**
	 * @param numeroEntrepriseCivile numéro de l'entreprise recherchée
	 * @param noEntreprises tableau contenant les numéros de contribuables des entreprises trouvées
	 */
	public PlusieursEntreprisesAvecMemeNumeroCivilException(long numeroEntrepriseCivile, long[] noEntreprises) {
		this.numeroEntrepriseCivile = numeroEntrepriseCivile;
		this.noEntreprises = noEntreprises;
		Arrays.sort(this.noEntreprises);
	}

	@Override
	public String getMessage() {
		return String.format("Plusieurs entreprises non-annulées partagent le même numéro d'entreprise %d: (%s)", numeroEntrepriseCivile, Arrays.toString(noEntreprises));
	}
}

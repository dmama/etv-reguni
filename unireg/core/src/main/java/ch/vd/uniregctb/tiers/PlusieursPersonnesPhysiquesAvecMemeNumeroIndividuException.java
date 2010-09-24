package ch.vd.uniregctb.tiers;

import java.util.Arrays;

/**
 * Exception lancée par le TiersDAO lorsque l'on cherche un tiers par numéro
 * d'individu et que l'on en trouve plusieurs (tous non-annulés, bien-sûr)
 */
public class PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException extends RuntimeException {

	private final long numeroIndividu;
	private final long[] noPersonnesPhysiques;

	/**
	 * @param numeroIndividu numéro de l'individu recherché
	 * @param noPersonnesPhysiques tableau contenant les numéros de contribuables des personnes physiques trouvées
	 */
	public PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException(long numeroIndividu, long[] noPersonnesPhysiques) {
		this.numeroIndividu = numeroIndividu;
		this.noPersonnesPhysiques = noPersonnesPhysiques;
	}

	@Override
	public String getMessage() {
		return String.format("Plusieurs tiers non-annulés partagent le même numéro d'individu %d (%s)", numeroIndividu, Arrays.toString(noPersonnesPhysiques));
	}
}

package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Couple d'une civilité (potentiellement vide) associée à un nom ou une raison sociale
 */
public class NomAvecCivilite {

	private final String civilite;
	private final String nomRaisonSociale;

	public NomAvecCivilite(String civilite, String nomRaisonSociale) {
		this.civilite = civilite;
		this.nomRaisonSociale = nomRaisonSociale;
	}

	public String getCivilite() {
		return civilite;
	}

	public String getNomRaisonSociale() {
		return nomRaisonSociale;
	}

	/**
	 * @return une version aggrégée de la civilité et du nom donné
	 */
	public String merge() {
		return Stream.of(civilite, nomRaisonSociale)
				.map(StringUtils::trimToNull)
				.filter(Objects::nonNull)
				.collect(Collectors.joining(" "));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final NomAvecCivilite that = (NomAvecCivilite) o;
		return Objects.equals(civilite, that.civilite) && Objects.equals(nomRaisonSociale, that.nomRaisonSociale);
	}

	@Override
	public int hashCode() {
		int result = civilite != null ? civilite.hashCode() : 0;
		result = 31 * result + (nomRaisonSociale != null ? nomRaisonSociale.hashCode() : 0);
		return result;
	}
}

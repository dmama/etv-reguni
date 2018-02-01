package ch.vd.unireg.registrefoncier;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * L'identifiant métier d'un droit sur un immeuble du registre foncier.
 */
public class IdentifiantDroitRF {

	/**
	 * Le numéro de l'office foncier
	 */
	private int numeroOffice;

	/**
	 * L'année de traitement.
	 */
	private int annee;

	/**
	 * Le numéro de traitement.
	 */
	private int numero;

	public IdentifiantDroitRF() {
	}

	public IdentifiantDroitRF(int numeroOffice, int annee, int numero) {
		this.numeroOffice = numeroOffice;
		this.annee = annee;
		this.numero = numero;
	}

	public IdentifiantDroitRF(@NotNull IdentifiantDroitRF right) {
		this.numeroOffice = right.numeroOffice;
		this.annee = right.annee;
		this.numero = right.numero;
	}

	public int getNumeroOffice() {
		return numeroOffice;
	}

	public void setNumeroOffice(int numeroOffice) {
		this.numeroOffice = numeroOffice;
	}

	public int getAnnee() {
		return annee;
	}

	public void setAnnee(int annee) {
		this.annee = annee;
	}

	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final IdentifiantDroitRF that = (IdentifiantDroitRF) o;
		return numeroOffice == that.numeroOffice &&
				annee == that.annee &&
				numero == that.numero;
	}

	@Override
	public int hashCode() {
		return Objects.hash(numeroOffice, annee, numero);
	}

	@Override
	public String toString() {
		return String.format("%03d-%4d/%06d", numeroOffice, annee, numero);
	}

	private static final Pattern PATTERN = Pattern.compile("([0-9]{3})-([0-9]{4})/([0-9]{6})");

	@Nullable
	public static IdentifiantDroitRF parse(@Nullable String value) {

		if (StringUtils.isBlank(value)) {
			return null;
		}

		final Matcher matcher = PATTERN.matcher(value);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("La string [" + value + "] ne représente pas un identifiant de droit RF valide");
		}

		int numeroOffice = Integer.parseInt(matcher.group(1));
		int annee = Integer.parseInt(matcher.group(2));
		int numero = Integer.parseInt(matcher.group(3));

		return new IdentifiantDroitRF(numeroOffice, annee, numero);
	}
}

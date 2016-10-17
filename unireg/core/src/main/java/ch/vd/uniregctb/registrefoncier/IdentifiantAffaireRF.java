package ch.vd.uniregctb.registrefoncier;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * L'identifiant métier d'une affaire du registre foncier.
 */
public class IdentifiantAffaireRF {

	/**
	 * Le numéro de l'office foncier
	 */
	private int numeroOffice;

	/**
	 * L'année de l'affaire.
	 */
	private int annee;

	/**
	 * Le numéro de l'affaire.
	 */
	private int numero;

	/**
	 * L'index de l'affaire.
	 */
	private int index;

	public IdentifiantAffaireRF() {
	}

	public IdentifiantAffaireRF(int numeroOffice, int annee, int numero, int index) {
		this.numeroOffice = numeroOffice;
		this.annee = annee;
		this.numero = numero;
		this.index = index;
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

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final IdentifiantAffaireRF that = (IdentifiantAffaireRF) o;
		return numeroOffice == that.numeroOffice &&
				annee == that.annee &&
				numero == that.numero &&
				index == that.index;
	}

	@Override
	public int hashCode() {
		return Objects.hash(numeroOffice, annee, numero, index);
	}

	@Override
	public String toString() {
		return String.format("%03d-%4d/%d/%d", numeroOffice, annee, numero, index);
	}

	private static final Pattern PATTERN = Pattern.compile("([0-9]{3})-([0-9]{4})/([0-9]+)/([0-9]+)");

	@Nullable
	public static IdentifiantAffaireRF parse(@Nullable String value) {

		if (StringUtils.isBlank(value)) {
			return null;
		}

		final Matcher matcher = PATTERN.matcher(value);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("La string [" + value + "] ne représente pas un identifiant d'affaire RF valide");
		}

		int numeroOffice = Integer.parseInt(matcher.group(1));
		int annee = Integer.parseInt(matcher.group(2));
		int numero = Integer.parseInt(matcher.group(3));
		int index = Integer.parseInt(matcher.group(4));

		return new IdentifiantAffaireRF(numeroOffice, annee, numero, index);
	}
}

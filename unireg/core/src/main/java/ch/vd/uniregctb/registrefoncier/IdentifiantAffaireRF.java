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
	@Nullable
	private Integer annee;

	/**
	 * Le numéro de l'affaire.
	 */
	@Nullable
	private Integer numero;

	/**
	 * L'index de l'affaire.
	 */
	@Nullable
	private Integer index;

	public IdentifiantAffaireRF() {
	}

	public IdentifiantAffaireRF(int numeroOffice, @Nullable Integer annee, @Nullable Integer numero, @Nullable Integer index) {
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

	@Nullable
	public Integer getAnnee() {
		return annee;
	}

	public void setAnnee(@Nullable Integer annee) {
		this.annee = annee;
	}

	@Nullable
	public Integer getNumero() {
		return numero;
	}

	public void setNumero(@Nullable Integer numero) {
		this.numero = numero;
	}

	@Nullable
	public Integer getIndex() {
		return index;
	}

	public void setIndex(@Nullable Integer index) {
		this.index = index;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final IdentifiantAffaireRF that = (IdentifiantAffaireRF) o;
		return numeroOffice == that.numeroOffice &&
				Objects.equals(annee, that.annee) &&
				Objects.equals(numero, that.numero) &&
				Objects.equals(index, that.index);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numeroOffice, annee, numero, index);
	}

	@Override
	public String toString() {
		if (annee == null || numero == null || index == null) { // une analyse des données à montré que si l'année est nulle, alors le numéro et l'index le sont aussi.
			return String.format("%03d", numeroOffice);
		}
		else {
			return String.format("%03d-%4d/%d/%d", numeroOffice, annee, numero, index);
		}
	}

	private static final Pattern PATTERN = Pattern.compile("([0-9]{3})(?:-([0-9]{4})/([0-9]+)/([0-9]+))?");

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
		Integer annee = null;
		Integer numero = null;
		Integer index = null;

		final String anneeAsString = matcher.group(2);
		if (StringUtils.isNotBlank(anneeAsString)) {
			annee = Integer.parseInt(anneeAsString);
			numero = Integer.parseInt(matcher.group(3));
			index = Integer.parseInt(matcher.group(4));
		}

		return new IdentifiantAffaireRF(numeroOffice, annee, numero, index);
	}
}

package ch.vd.unireg.registrefoncier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * L'identifiant métier d'une affaire du registre foncier.
 */
public class IdentifiantAffaireRF implements Comparable<IdentifiantAffaireRF> {

	/**
	 * Le numéro de l'office foncier
	 */
	private int numeroOffice;

	/**
	 * Le numéro d'affaire en format texte libre.
	 */
	@Nullable
	private String numeroAffaire;

	public IdentifiantAffaireRF() {
	}

	public IdentifiantAffaireRF(int numeroOffice, @Nullable Integer annee, @Nullable Integer numero, @Nullable Integer index) {
		this.numeroOffice = numeroOffice;
		this.numeroAffaire = buildNumeroAffaire(annee, numero, index);
	}

	public IdentifiantAffaireRF(int numeroOffice, @Nullable String numeroAffaire) {
		this.numeroOffice = numeroOffice;
		this.numeroAffaire = numeroAffaire;
	}

	public IdentifiantAffaireRF(@NotNull IdentifiantAffaireRF right) {
		this.numeroOffice = right.numeroOffice;
		this.numeroAffaire = right.numeroAffaire;
	}

	@Nullable
	private static String buildNumeroAffaire(@Nullable Integer annee, @Nullable Integer numero, @Nullable Integer index) {
		if (annee == null && numero == null && index == null) {
			return null;
		}
		final List<String> values = new ArrayList<>(3);
		Optional.ofNullable(annee).ifPresent(v -> values.add(String.valueOf(v)));
		Optional.ofNullable(numero).ifPresent(v -> values.add(String.valueOf(v)));
		Optional.ofNullable(index).ifPresent(v -> values.add(String.valueOf(v)));
		return String.join("/", values);
	}

	public int getNumeroOffice() {
		return numeroOffice;
	}

	public void setNumeroOffice(int numeroOffice) {
		this.numeroOffice = numeroOffice;
	}

	@Nullable
	public String getNumeroAffaire() {
		return numeroAffaire;
	}

	public void setNumeroAffaire(@Nullable String numeroAffaire) {
		this.numeroAffaire = numeroAffaire;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final IdentifiantAffaireRF that = (IdentifiantAffaireRF) o;
		return numeroOffice == that.numeroOffice &&
				Objects.equals(numeroAffaire, that.numeroAffaire);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numeroOffice, numeroAffaire);
	}

	@Override
	public String toString() {
		if (numeroAffaire == null) {
			return String.format("%03d", numeroOffice);
		}
		else {
			return String.format("%03d-%s", numeroOffice, numeroAffaire);
		}
	}

	private static final Pattern PATTERN = Pattern.compile("([0-9]{3})(?:-(.*))?");

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
		final String numeroAffaire = matcher.group(2);

		return new IdentifiantAffaireRF(numeroOffice, numeroAffaire);
	}

	@Override
	public int compareTo(@NotNull IdentifiantAffaireRF o) {
		int c = Integer.compare(this.numeroOffice, o.numeroOffice);
		if (c != 0) {
			return c;
		}
		c = Objects.compare(this.numeroAffaire, o.numeroAffaire, Comparator.nullsFirst(Comparator.naturalOrder()));
		return c;
	}
}

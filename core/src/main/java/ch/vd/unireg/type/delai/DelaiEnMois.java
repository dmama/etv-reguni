package ch.vd.unireg.type.delai;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * Un délai qui s'exprime en mois.
 */
public class DelaiEnMois extends Delai {

	public static final Pattern STRING_PATTERN = Pattern.compile("(-?\\d+)M(~)?");

	private final int mois;
	private final boolean reportFinMois;

	/**
	 * @param mois          le nombre de mois de délai
	 * @param reportFinMois <i>vrai</i> s'il faut reporter la date calculée à la fin du mois (28.02.2003 + 1 mois => 31.03.2003); <i>faux</i> s'il faut appliquer le délai sans ajustement (28.02.2003 + 1 mois => 28.03.2003).
	 */
	public DelaiEnMois(int mois, boolean reportFinMois) {
		this.mois = mois;
		this.reportFinMois = reportFinMois;
	}

	/**
	 * @return le nombre de mois de délai.
	 */
	public int getMois() {
		return mois;
	}

	/**
	 * @return <i>vrai</i> s'il faut reporter la date calculée à la fin du mois (28.02.2003 + 1 mois => 31.03.2003); <i>faux</i> s'il faut appliquer le délai sans ajustement (28.02.2003 + 1 mois => 28.03.2003).
	 */
	public boolean isReportFinMois() {
		return reportFinMois;
	}

	@Override
	public @NotNull RegDate apply(@NotNull RegDate date) {
		final RegDate dateDecalee = date.addMonths(mois);
		if (reportFinMois) {
			return dateDecalee.getLastDayOfTheMonth();
		}
		else {
			return dateDecalee;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final DelaiEnMois that = (DelaiEnMois) o;
		return mois == that.mois &&
				reportFinMois == that.reportFinMois;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mois, reportFinMois);
	}

	@Override
	public String toString() {
		return mois + "M" + (reportFinMois ? "~" : "");
	}

	/**
	 * Parse la représentation string d'un délai en mois (e.g. "6 mois") et construit l'objet correspondant.
	 *
	 * @param string une string qui doit représenter un délai.
	 * @return l'objet {@link DelaiEnMois} correspondant.
	 * @throws IllegalArgumentException en cas d'erreur de parsing
	 */
	@NotNull
	public static DelaiEnMois fromString(@NotNull String string) {
		string = string.trim();

		final Matcher matcher = STRING_PATTERN.matcher(string);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Le délai [" + string + "] n'est pas valide");
		}

		final int mois = Integer.parseInt(matcher.group(1));
		final boolean reportFinMois = (matcher.group(2) != null);

		return new DelaiEnMois(mois, reportFinMois);
	}
}

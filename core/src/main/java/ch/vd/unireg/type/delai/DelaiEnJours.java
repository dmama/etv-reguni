package ch.vd.unireg.type.delai;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * Un délai qui s'exprime en jours.
 */
public class DelaiEnJours extends Delai {

	public static final Pattern STRING_PATTERN = Pattern.compile("(-?\\d+)D(~)?");

	private final int jours;
	private final boolean reportFinMois;

	/**
	 * @param jours         le nombre de jours de délai
	 * @param reportFinMois <i>vrai</i> s'il faut reporter la date calculée à la fin du mois (28.02.2003 + 30 jours => 31.03.2003); <i>faux</i> s'il faut appliquer le délai sans ajustement (28.02.2003 + 30 jours => 30.03.2003).
	 */
	public DelaiEnJours(int jours, boolean reportFinMois) {
		this.jours = jours;
		this.reportFinMois = reportFinMois;
	}

	public DelaiEnJours(@NotNull DelaiEnJours right) {
		this.jours = right.getJours();
		this.reportFinMois = right.isReportFinMois();
	}

	/**
	 * @return le nombre de jours de délai.
	 */
	public int getJours() {
		return jours;
	}

	/**
	 * @return <i>vrai</i> s'il faut reporter la date calculée à la fin du mois (28.02.2003 + 30 jours => 31.03.2003); <i>faux</i> s'il faut appliquer le délai sans ajustement (28.02.2003 + 30 jours => 30.03.2003).
	 */
	public boolean isReportFinMois() {
		return reportFinMois;
	}

	@Override
	@NotNull
	public RegDate apply(@NotNull RegDate date) {
		final RegDate dateDecalee = date.addDays(jours);
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
		final DelaiEnJours that = (DelaiEnJours) o;
		return jours == that.jours &&
				reportFinMois == that.reportFinMois;
	}

	@Override
	public int hashCode() {
		return Objects.hash(jours, reportFinMois);
	}

	@Override
	public String toString() {
		return jours + "D" + (reportFinMois ? "~" : "");
	}

	/**
	 * Parse la représentation string d'un délai en jours (e.g. "6 jours") et construit l'objet correspondant.
	 *
	 * @param string une string qui doit représenter un délai en jours.
	 * @return l'objet {@link DelaiEnJours} correspondant.
	 * @throws IllegalArgumentException en cas d'erreur de parsing
	 */
	@NotNull
	public static DelaiEnJours fromString(@NotNull String string) {
		string = string.trim();

		final Matcher matcher = STRING_PATTERN.matcher(string);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Le délai [" + string + "] n'est pas valide");
		}

		final int jours = Integer.parseInt(matcher.group(1));
		final boolean reportFinMois = (matcher.group(2) != null);

		return new DelaiEnJours(jours, reportFinMois);
	}

	@Override
	public DelaiEnJours duplicate() {
		return new DelaiEnJours(this);
	}
}

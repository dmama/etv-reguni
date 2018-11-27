package ch.vd.unireg.type.delai;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Duplicable;

/**
 * Un délai qui permet de tranformer une {@link RegDate} en une autre.
 */
public abstract class Delai implements Duplicable<Delai> {

	/**
	 * Applique le délai à la date spécifiée.
	 *
	 * @param date une date
	 * @return la date avec le délai appliqué.
	 */
	@NotNull
	public abstract RegDate apply(@NotNull RegDate date);

	/**
	 * Parse la représentation string d'un délai et construit l'objet correspondant.
	 *
	 * @param string une string qui doit représenter un délai.
	 * @return l'objet {@link Delai} correspondant.
	 * @throws IllegalArgumentException en cas d'erreur de parsing
	 */
	@NotNull
	public static Delai fromString(@NotNull String string) {
		string = string.trim();

		if (DelaiEnJours.STRING_PATTERN.matcher(string).matches()) {
			return DelaiEnJours.fromString(string);
		}
		else if (DelaiEnMois.STRING_PATTERN.matcher(string).matches()) {
			return DelaiEnMois.fromString(string);
		}
		else if (DelaiComposite.STRING_PATTERN.matcher(string).matches()) {
			return DelaiComposite.fromString(string);
		}
		else {
			throw new IllegalArgumentException("Le délai [" + string + "] n'est pas valide");
		}
	}
}

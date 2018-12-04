package ch.vd.unireg.type.delai;

import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

public class DelaiHelper {

	public static String toDisplayString(@NotNull Delai delai) {
		if (delai instanceof DelaiEnJours) {
			final DelaiEnJours d = (DelaiEnJours) delai;
			return d.getJours() + " jours" + (d.isReportFinMois() ? " (avec report de fin de mois)" : "");
		}
		else if (delai instanceof DelaiEnMois) {
			final DelaiEnMois d = (DelaiEnMois) delai;
			return d.getMois() + " mois" + (d.isReportFinMois() ? " (avec report de fin de mois)" : "");
		}
		else if (delai instanceof DelaiComposite) {
			final DelaiComposite d = (DelaiComposite) delai;
			return d.getComposants().stream()
					.map(DelaiHelper::toDisplayString)
					.collect(Collectors.joining(" + "));
		}
		else {
			throw new IllegalArgumentException("Type de délai inconnu = [" + delai.getClass() + "]");
		}
	}
}

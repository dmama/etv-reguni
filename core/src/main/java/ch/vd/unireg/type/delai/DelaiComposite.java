package ch.vd.unireg.type.delai;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * Un délai composé de plusieurs autres délais appliqués successivement.
 */
public class DelaiComposite extends Delai {

	public static final Pattern STRING_PATTERN = Pattern.compile(".+\\+.+");

	private final List<Delai> composants;

	public DelaiComposite(@NotNull List<Delai> composants) {
		if (composants.size() <= 1) {
			throw new IllegalArgumentException("Il doit y avoir au moins 2 composants");
		}
		this.composants = Collections.unmodifiableList(composants);
	}

	public DelaiComposite(Delai... composants) {
		this(Arrays.asList(composants));
	}

	/**
	 * @return les sous-délais qui composent ce délai.
	 */
	public List<Delai> getComposants() {
		return composants;
	}

	@Override
	public @NotNull RegDate apply(@NotNull RegDate date) {
		RegDate dateDecalee = date;
		for (Delai composant : composants) {
			dateDecalee = composant.apply(dateDecalee);
		}
		return dateDecalee;
	}

	@Override
	public String toString() {
		return composants.stream()
				.map(Delai::toString)
				.collect(Collectors.joining(" + "));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final DelaiComposite that = (DelaiComposite) o;
		return Objects.equals(composants, that.composants);
	}

	@Override
	public int hashCode() {
		return Objects.hash(composants);
	}

	@NotNull
	public static DelaiComposite fromString(@NotNull String string) {

		if (!STRING_PATTERN.matcher(string).matches()) {
			throw new IllegalArgumentException("Le délai [" + string + "] n'est pas valide");
		}

		final List<@NotNull Delai> list = Arrays.stream(string.split("\\+"))
				.filter(StringUtils::isNotBlank)
				.map(Delai::fromString)
				.collect(Collectors.toList());
		return new DelaiComposite(list);
	}
}

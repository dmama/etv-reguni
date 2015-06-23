package ch.vd.uniregctb.migration.pm.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.CollectionsUtils;

/**
 * Classe des éléments composites de logs CSV-like
 */
public class CompositeLoggedElement implements LoggedElement {

	/**
	 * Constante interne qui sert de point de départ vide
	 */
	private static final LoggedElement EMPTY = new EmptyValuedLoggedElement();

	private final List<LoggedElementAttribute> items;
	private final Map<LoggedElementAttribute, Object> values;

	public CompositeLoggedElement(LoggedElement... elements) {
		this(elements == null ? Collections.emptyList() : Arrays.asList(elements));
	}

	public CompositeLoggedElement(List<LoggedElement> elements) {
		final LoggedElement elt = buildComposite(elements);
		this.items = Collections.unmodifiableList(elt.getItems());
		this.values = Collections.unmodifiableMap(elt.getItemValues());
	}

	@NotNull
	@Override
	public final List<LoggedElementAttribute> getItems() {
		return items;
	}

	@NotNull
	@Override
	public final Map<LoggedElementAttribute, Object> getItemValues() {
		return values;
	}

	@NotNull
	private static LoggedElement buildComposite(List<LoggedElement> elements) {
		LoggedElement composite = EMPTY;
		for (LoggedElement elt : CollectionsUtils.revertedOrder(elements)) {
			composite = compose(elt, composite);
		}
		return composite;
	}

	/**
	 * @param suffix instance de {@link LoggedElement} à ajouter (après) les <i>items</i> courant
	 * @return nouvelle instance composite
	 */
	@NotNull
	public static LoggedElement compose(final LoggedElement prefix, final LoggedElement suffix) {

		return new LoggedElement() {

			@NotNull
			@Override
			public List<LoggedElementAttribute> getItems() {
				final List<LoggedElementAttribute> base = prefix.getItems();
				final List<LoggedElementAttribute> added = suffix.getItems();
				if (added.isEmpty()) {
					return base;
				}
				else if (base.isEmpty()) {
					return added;
				}

				// on va conserver l'ordre tout en supprimant les doublons
				final Set<LoggedElementAttribute> composition = new LinkedHashSet<>(base.size() + added.size());
				composition.addAll(base);
				composition.addAll(added);
				return new ArrayList<>(composition);
			}

			@NotNull
			@Override
			public Map<LoggedElementAttribute, Object> getItemValues() {
				final Map<LoggedElementAttribute, Object> base = prefix.getItemValues();
				final Map<LoggedElementAttribute, Object> added = suffix.getItemValues();
				if (added.isEmpty()) {
					return base;
				}
				else if (base.isEmpty()) {
					return added;
				}

				final Map<LoggedElementAttribute, Pair<LoggedElementAttribute, Object>> whole = Stream.concat(base.entrySet().stream(), added.entrySet().stream())
						.filter(p -> p.getValue() != null)
						.collect(Collectors.toMap(Map.Entry::getKey,
						                          e -> Pair.of(e.getKey(), e.getValue()),
						                          (e1, e2) -> merge(e1.getKey(), e1.getValue(), e2.getValue()),
						                          () -> new EnumMap<>(LoggedElementAttribute.class)));

				return whole.entrySet().stream()
						.filter(e -> e.getValue().getValue() != null)
						.collect(Collectors.toMap(Map.Entry::getKey,
						                          e -> e.getValue().getValue(),
						                          (v1, v2) -> { throw new IllegalStateException("Merger should not be called!!"); },
						                          () -> new EnumMap<>(LoggedElementAttribute.class)));
			}
		};
	}

	/**
	 * Opération de merge entre deux valeurs en utilisant le merger défini pour l'item
	 */
	@NotNull
	private static <T> Pair<LoggedElementAttribute, T> merge(LoggedElementAttribute item, T v1, T v2) {
		if (v1 == null) {
			return Pair.of(item, v2);
		}
		if (v2 == null || v1.equals(v2)) {
			return Pair.of(item, v1);
		}

		final BinaryOperator<T> merger = item.getValueMerger();
		try {
			return Pair.of(item, merger.apply(v1, v2));
		}
		catch (LoggedElementHelper.IncompabibleValuesException e) {
			// histoire de savoir un peu mieux d'où vient le souci...
			throw new LoggedElementHelper.IncompabibleValuesException(String.format("Concept %s: %s", item, e.getMessage()), e);
		}
	}

}

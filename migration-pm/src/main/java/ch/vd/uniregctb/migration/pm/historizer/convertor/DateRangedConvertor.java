package ch.vd.uniregctb.migration.pm.historizer.convertor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

public class DateRangedConvertor {

	/**
	 * Take each {@link DateRanged} of the list and create an identical {@link DateRanged} with the payload T
	 * converted into R by the convertor {@link Function}.
	 * @param ranges The incoming {@link List} of {@link DateRanged}
	 * @param convertor The convertor {@link Function} used to convert the payload.
	 * @param <T> The incoming payload type.
	 * @param <R> The outgoing payload type.
	 * @return A new {@link List} of {@link DateRanged} with the converted payload.
	 */
	public static <T, R> List<DateRanged<R>> convert(List<DateRanged<T>> ranges, Function<T, R> convertor) {
		return ranges.stream()
				.map(r -> convert(r, convertor))
				.collect(Collectors.toList());
	}

	public static <T, R> DateRanged<R> convert(DateRanged<T> range, Function<T, R> convertor) {
		return new DateRanged<>(range.getDateDebut(), range.getDateFin(), convertor.apply(range.getPayload()));
	}
}

package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.convertor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.DateRangeHelper;

public class DateRangedConvertor {

	/**
	 * Take each {@link DateRangeHelper.Ranged} of the list and create an identical {@link DateRangeHelper.Ranged} with the payload T
	 * converted into R by the convertor {@link Function}.
	 * @param ranges The incoming {@link List} of {@link DateRangeHelper.Ranged}
	 * @param convertor The convertor {@link Function} used to convert the payload.
	 * @param <T> The incoming payload type.
	 * @param <R> The outgoing payload type.
	 * @return A new {@link List} of {@link DateRangeHelper.Ranged} with the converted payload.
	 */
	public static <T, R> List<DateRangeHelper.Ranged<R>> convert(List<DateRangeHelper.Ranged<T>> ranges, Function<? super T, ? extends R> convertor) {
		return ranges.stream()
				.map(r -> DateRangedConvertor.<T,R>map(r, convertor))           // ce "cast" est en fait inutile, mais Idea 2017.1.4 semble en avoir besoin...
				.collect(Collectors.toList());
	}

	public static <S, D> DateRangeHelper.Ranged<D> map(DateRangeHelper.Ranged<S> src, Function<? super S, ? extends D> mapper) {
		return src.withPayload(mapper.apply(src.getPayload()));
	}
}

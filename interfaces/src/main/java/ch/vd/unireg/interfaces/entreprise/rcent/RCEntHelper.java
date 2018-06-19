package ch.vd.unireg.interfaces.entreprise.rcent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;

public class RCEntHelper {

	/**
	 * Ré-enveloppe la donnée d'un DateRanged RCEnt dans un DateRanged Unireg.
	 * @param rcEntDr Un DateRanged RCEnt.
	 * @param <U> Le type de la donnée enveloppée dans le DateRanged.
	 * @return Un nouveau DateRanged Unireg avec la donnée.
	 */
	public static <U> DateRanged<U> convert(DateRangeHelper.Ranged<U> rcEntDr) {
		if (rcEntDr == null) {
			return null;
		}
		return new DateRanged<>(rcEntDr.getDateDebut(), rcEntDr.getDateFin(), rcEntDr.getPayload());
	}

	/**
	 * Converti une liste de DateRanged RCEnt en liste de DateRanged Unireg, en conservant la donnée originale de chacun.
	 * @param rcEntDrList Une liste de DateRanged RCEnt.
	 * @param <U> Le type de la donnée enveloppée dans les DateRanged de la liste.
	 * @return Une nouvelle liste de DateRanged Unireg, avec les données.
	 */
	public static <U> List<DateRanged<U>> convert(List<DateRangeHelper.Ranged<U>> rcEntDrList) {
		if (rcEntDrList == null) {
			return null;
		}
		final List<DateRanged<U>> drList = new ArrayList<>(rcEntDrList.size());
		for (DateRangeHelper.Ranged<U> dr : rcEntDrList) {
			drList.add(convert(dr));
		}
		return drList;
	}

	/**
	 * Converti une {@link Map} de listes RCEnt en Map de liste de DateRanged Unireg, en conservant la donnée originale de
	 * chaque DateRanged.
	 * @param rcEntDrListMap Une Map de listes de DateRanged RCEnt.
	 * @param <K> Le type des clés de la Map.
	 * @param <U> Le type de la donnée enveloppée dans les DateRanged des listes.
	 * @return Une nouvelle Map de listes de DateRanged Unireg, avec les données.
	 */
	public static <K, U> Map<K, List<DateRanged<U>>> convert(Map<K, List<DateRangeHelper.Ranged<U>>> rcEntDrListMap) {
		if (rcEntDrListMap == null) {
			return null;
		}
		final Map<K, List<DateRanged<U>>> map = new HashMap<>(rcEntDrListMap.size());
		for (Map.Entry<K, List<DateRangeHelper.Ranged<U>>> e : rcEntDrListMap.entrySet()) {
			map.put(e.getKey(), convert(e.getValue()));
		}
		return map;
	}

	/**
	 * Ré-enveloppe la donnée d'un DateRanged RCEnt dans un DateRanged Unireg. Au passage,
	 * transforme la donnée au moyen de la fonction de conversion.
	 * @param rcEntDr Un DateRanged RCEnt.
	 * @param mapper Une fonction de transformation de la donnée.
	 * @param <R> Le type de la donnée enveloppée dans le DateRanged Unireg en sortie.
	 * @param <U> Le type de la donnée enveloppée dans le DateRanged RCEnt en entrée.
	 * @return
	 */
	public static <R, U> DateRanged<R> convertAndMap(DateRangeHelper.Ranged<U> rcEntDr,
	                                                 Function<? super U, ? extends R> mapper) {
		if (rcEntDr == null) {
			return null;
		}
		return new DateRanged<>(rcEntDr.getDateDebut(), rcEntDr.getDateFin(), mapper.apply(rcEntDr.getPayload()));
	}

	/**
	 * Converti une liste de DateRanged RCEnt en DateRanged Unireg. Au passage,
	 * transforme les données au moyen de la fonction de conversion.
	 * @param rcEntDrList Une liste de DateRanged RCEnt.
	 * @param mapper Une fonction de transformation des données.
	 * @param <U> Le type des donnée enveloppée dans les DateRanged Unireg en sortie.
	 * @param <R> Le type des donnée enveloppée dans les DateRanged RCEnt en entrée.
	 * @return Une nouvelle liste de DateRanged Unireg, avec les données transformées.
	 */
	public static <U, R> List<DateRanged<R>> convertAndMap(List<DateRangeHelper.Ranged<U>> rcEntDrList,
	                                                       Function<? super U, ? extends R> mapper) {
		if (rcEntDrList == null) {
			return null;
		}
		final List<DateRanged<R>> drList = new ArrayList<>(rcEntDrList.size());
		for (DateRangeHelper.Ranged<U> dr : rcEntDrList) {
			drList.add(convertAndMap(dr, mapper));
		}
		return drList;
	}

	/**
	 * Converti une {@link Map} de listes RCEnt en Map de liste de DateRanged Unireg, en conservant la donnée originale de
	 * chaque DateRanged.
	 * @param rcEntDrListMap Une Map de listes de DateRanged RCEnt.
	 * @param mapper Une fonction de transformation des données.
	 * @param <K> Le type des clés de la Map.
	 * @param <U> Le type de la donnée enveloppée dans les DateRanged des listes.
	 * @param <R> Le type des donnée enveloppée dans les DateRanged RCEnt en entrée.
	 * @return Une nouvelle Map de listes de DateRanged Unireg, avec les données.
	 */
	public static <K, U, R> Map<K, List<DateRanged<R>>> convertAndMap(Map<K, List<DateRangeHelper.Ranged<U>>> rcEntDrListMap,
	                                                                  Function<? super U, ? extends R> mapper) {
		if (rcEntDrListMap == null) {
			return null;
		}
		final Map<K, List<DateRanged<R>>> map = new HashMap<>(rcEntDrListMap.size());
		for (Map.Entry<K, List<DateRangeHelper.Ranged<U>>> e : rcEntDrListMap.entrySet()) {
			map.put(e.getKey(), convertAndMap(e.getValue(), mapper));
		}
		return map;
	}

	/**
	 * Converti une liste de DateRanged RCEnt en liste d'entité Unireg. En principe, la donnée Unireg reprend le début et la fin de chaque
	 * période DateRanged. On peut préfiltrer les données sur la base d'un prédicat optionnel.
	 * @param source Une liste de DateRanged RCEnt.
	 * @param flatMapper Une fonction de transformation des données.
	 * @param filterPredicate Un prédicat de filtrage sur le type S des données en entrée.
	 * @param <S> Le type des données en entrée.
	 * @param <D> Le type des données en sortie.
	 * @return Une nouvelle liste contenant les données transformées.
	 */
	public static <S, D extends DateRange> List<D> convertAndDerange(List<? extends DateRangeHelper.Ranged<S>> source,
	                                                                 Function<? super DateRangeHelper.Ranged<S>, ? extends D> flatMapper,
	                                                                 @Nullable Predicate<? super S> filterPredicate) {
		if (source == null) {
			return null;
		}
		if (source.isEmpty()) {
			return Collections.emptyList();
		}

		final List<D> resultat = new ArrayList<>(source.size());
		for (DateRangeHelper.Ranged<S> src : source) {
			if (shouldMap(filterPredicate, src)) {
				final D mapped = flatMapper.apply(src);
				if (mapped != null) {
					resultat.add(mapped);
				}
			}
		}
		return resultat;
	}

	private static <S> boolean shouldMap(@Nullable Predicate<? super S> filterPredicate, DateRangeHelper.Ranged<S> src) {
		return filterPredicate == null || filterPredicate.test(src.getPayload());
	}

	public static <S, D extends DateRange> List<D> convertAndDerange(List<? extends DateRangeHelper.Ranged<S>> source,
	                                                                 Function<? super DateRangeHelper.Ranged<S>, ? extends D> flatMapper) {
		return convertAndDerange(source, flatMapper, null);
	}

	/**
	 * Converti une {@link Map} de listes RCEnt en Map de liste de données Unireg, en transformant la donnée originale de chaque DateRanged à
	 * l'aide du flatMapper. La donnée Unireg implément DateRange et reprend le début et la fin de chaque période DateRanged.
	 *
	 * @param rcEntDrListMap Une Map de listes de DateRanged RCEnt.
	 * @param flatMapper Une fonction de transformation des données.
	 * @param <K> Le type des clés de la Map.
	 * @param <U> Le type de la donnée enveloppée dans les DateRanged des listes.
	 * @param <R> Le type des donnée enveloppée dans les DateRanged RCEnt en entrée.
	 * @return Une nouvelle Map de listes de données Unireg.
	 */
	public static <K, U, R extends DateRange> Map<K, List<R>>  convertAndMapDerange(Map<K, List<DateRangeHelper.Ranged<U>>> rcEntDrListMap,
	                                                                                Function<? super DateRangeHelper.Ranged<U>, ? extends R> flatMapper) {
		if (rcEntDrListMap == null) {
			return null;
		}
		final Map<K, List<R>> map = new HashMap<>(rcEntDrListMap.size());
		for (Map.Entry<K, List<DateRangeHelper.Ranged<U>>> e : rcEntDrListMap.entrySet()) {
			map.put(e.getKey(), convertAndDerange(e.getValue(), flatMapper));
		}
		return map;
	}

	public static <S, D> List<D> convert(List<S> entityList, Function<? super S, ? extends D> mapper) {
		if (entityList == null) {
			return null;
		}
		final List<D> resultat = new ArrayList<>(entityList.size());
		for (S element : entityList) {
			final D converted = mapper.apply(element);
			if (converted != null) {
				resultat.add(converted);
			}
		}
		return resultat;
	}
}

package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.rcent.converters.Converter;

public class RCEntHelper {

	/**
	 * Ré-enveloppe la donnée d'un DateRanged RCEnt dans un DateRanged Unireg.
	 * @param rcEntDr Un DateRanged RCEnt.
	 * @param <U> Le type de la donnée enveloppée dans le DateRanged.
	 * @return Un nouveau DateRanged Unireg avec la donnée.
	 */
	public static <U> DateRanged<U> convert(ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U> rcEntDr) {
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
	public static <U> List<DateRanged<U>> convert(List<ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U>> rcEntDrList) {
		if (rcEntDrList == null) {
			return null;
		}
		List<DateRanged<U>> drList = new ArrayList<>(rcEntDrList.size());
		for (ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U> dr : rcEntDrList) {
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
	public static <K, U> Map<K, List<DateRanged<U>>> convert(Map<K, List<ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U>>> rcEntDrListMap) {
		if (rcEntDrListMap == null) {
			return null;
		}
		HashMap<K, List<DateRanged<U>>> map = new HashMap<>(rcEntDrListMap.size());
		for (Map.Entry<K, List<ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U>>> e : rcEntDrListMap.entrySet()) {
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
	public static <R, U> DateRanged<R> convertAndMap(ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U> rcEntDr,
	                                                  Converter<U, R> mapper) {
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
	public static <U, R> List<DateRanged<R>> convertAndMap(List<ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U>> rcEntDrList,
	                                                       Converter<U, R> mapper) {
		if (rcEntDrList == null) {
			return null;
		}
		List<DateRanged<R>> drList = new ArrayList<>(rcEntDrList.size());
		for (ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U> dr : rcEntDrList) {
			drList.add(
					convertAndMap(dr, mapper)
			);
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
	public static <K, U, R> Map<K, List<DateRanged<R>>> convertAndMap(Map<K, List<ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U>>> rcEntDrListMap,
	                                                            Converter<U, R> mapper) {
		if (rcEntDrListMap == null) {
			return null;
		}
		HashMap<K, List<DateRanged<R>>> map = new HashMap<>(rcEntDrListMap.size());
		for (Map.Entry<K, List<ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged<U>>> e : rcEntDrListMap.entrySet()) {
			map.put(e.getKey(), convertAndMap(e.getValue(), mapper));
		}
		return map;
	}
}

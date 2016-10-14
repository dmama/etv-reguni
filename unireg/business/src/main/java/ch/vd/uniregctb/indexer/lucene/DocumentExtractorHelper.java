package ch.vd.uniregctb.indexer.lucene;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.common.StringParser;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;

public abstract class DocumentExtractorHelper {

	public static boolean isBlank(String value) {
		return IndexerFormatHelper.isBlank(value);
	}

	public static RegDate indexStringToDate(String date, boolean allowPartial) {
		try {
			return RegDateHelper.StringFormat.INDEX.fromString(date, allowPartial, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		catch (ParseException e) {
			return null;
		}
	}

	/**
	 * Renvoie la valeur dans le document Lucene Ou chaine vide si non trouvé Ne renvoie jamais NULL
	 * (en cas de champ multivalué, seule la première valeur sera retournée)
	 *
	 * @param key clé de la valeur recherchée
	 * @param document document Lucene
	 * @return la valeur présente pour la clé donnée dans le document Lucene
	 */
	@NotNull
	public static String getDocValue(String key, Document document) {
		final String str = document.get(key);
		return isBlank(str) ? StringUtils.EMPTY : str;
	}

	/**
	 * Renvoie les valeurs dans le document Lucene (utile pour les champs multivalués)
	 *
	 * @param key clé de la valeur recherchée
	 * @param document document Lucene
	 * @return les valeurs présentes pour la clé donnée dans le document Lucene
	 */
	@NotNull
	public static String[] getDocValues(String key, Document document) {
		return document.getValues(key);
	}

	/**
	 * Renvoie la valeur dans le document Lucene Ou chaine vide si non trouvé Ne renvoie jamais NULL
	 *
	 * @param key      la clé sous laquelle est stocké la valeur
	 * @param document le document Lucene
	 * @return la valeur du document Lucene
	 */
	public static Long getLongValue(String key, Document document) {
		return getValue(key, document, Long::valueOf);
	}

	public static Boolean getBooleanValue(String key, Document document, @Nullable Boolean defaultValue) {
		final String str = document.get(key);
		if (isBlank(str)) {
			return defaultValue;
		}
		else {
			return Constants.OUI.equals(str);
		}
	}

	public static Integer getIntegerValue(String key, Document document) {
		return getValue(key, document, Integer::valueOf);
	}

	public static <T extends Enum<T>> T getEnumValue(String key, Document document, final Class<T> clazz) {
		return getValue(key, document, string -> Enum.valueOf(clazz, string));
	}

	public static RegDate getRegDateValue(String key, Document document, final boolean allowPartial) {
		return getValue(key, document, string -> {
			final int index = Integer.valueOf(string);
			return RegDate.fromIndex(index, allowPartial);
		});
	}

	public static <T> T getValue(String key, Document document, StringParser<T> parser) {
		final String str = document.get(key);
		return isBlank(str) ? null : parser.parse(str);
	}

	public static List<String> getList(String str) {
		if (isBlank(str)) {
			return Collections.emptyList();
		}

		final String[] splitted = StringUtils.split(str);
		return Arrays.asList(splitted);
	}

	public static <T extends Enum<T>> Set<T> getEnumSet(String[] array, Class<T> clazz) {
		if (array == null || array.length == 0) {
			return Collections.emptySet();
		}

		final Set<T> set = EnumSet.noneOf(clazz);
		for (String elt : array) {
			set.add(Enum.valueOf(clazz, elt));
		}
		return set;
	}
}

package ch.vd.uniregctb.indexer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.common.StringRenderer;

public class IndexerFormatHelper {

	private static final String NULL_VALUE = "NULL";

	private static final Pattern DOT_DASH_BLANK = Pattern.compile("[-.\\s]");

	/**
	 * Différents modes de transformation de dates en chaînes de caractères :
	 * <ul>
	 *     <li>{@link #INDEXATION} indique que la valeur sert à la construction d'un élément recherchable ;</li>
	 *     <li>{@link #STORAGE} indique que la valeur sert à la construction d'un élément pour le stockage simple (ou comme critère de recherche).</li>
	 * </ul>
	 */
	public enum DateStringMode {
		INDEXATION,
		STORAGE
	}

	private abstract static class AbstractStringRendererImpl<T> implements StringRenderer<T> {
		@Override
		public final String toString(T object) {
			return object != null ? toStringFromNotNull(object) : nullValue();
		}

		protected abstract String toStringFromNotNull(@NotNull T object);

		protected String nullValue() {
			return IndexerFormatHelper.nullValue();
		}
	}

	/**
	 * Permet de générer une chaîne de caractères à partir d'une collection d'éléments
	 * @param <T> le type des éléments en question
	 */
	private static class CollectionStringRenderer<T> extends AbstractStringRendererImpl<Collection<T>> {

		private final StringRenderer<? super T> renderer;

		public CollectionStringRenderer(StringRenderer<? super T> renderer) {
			this.renderer = renderer;
		}

		@Override
		protected String toStringFromNotNull(@NotNull Collection<T> collection) {
			final StringBuilder b = new StringBuilder();
			for (T obj : collection) {
				if (b.length() > 0) {
					b.append(' ');
				}
				b.append(renderer.toString(obj));
			}
			return b.toString();
		}
	}

	/**
	 * Utilisation de {@link #toString()} ou {@link #nullValue()}
	 */
	public static final StringRenderer<Object> DEFAULT_RENDERER = new AbstractStringRendererImpl<Object>() {
		@Override
		protected String toStringFromNotNull(@NotNull Object object) {
			return object.toString();
		}
	};

	/**
	 * Utilisation de {@link RegDateHelper#toIndexString(ch.vd.registre.base.date.RegDate)}
	 */
	public static final StringRenderer<RegDate> STORAGE_REGDATE_RENDERER = new AbstractStringRendererImpl<RegDate>() {
		@Override
		protected String toStringFromNotNull(@NotNull RegDate object) {
			return RegDateHelper.toIndexString(object);
		}
	};

	/**
	 * Utilisation de {@link #dateToSearchableString(ch.vd.registre.base.date.RegDate)}
	 */
	public static final StringRenderer<RegDate> INDEXATION_REGDATE_RENDERER = new AbstractStringRendererImpl<RegDate>() {
		@Override
		protected String toStringFromNotNull(@NotNull RegDate object) {
			return dateToSearchableString(object);
		}
	};

	/**
	 * Suppression des blancs, points et autres tirets (chaine {@link #NULL_VALUE} si l'original est <code>null</code>)
	 */
	public static final StringRenderer<String> AVS_RENDERER = new AbstractStringRendererImpl<String>() {
		@Override
		protected String toStringFromNotNull(@NotNull String object) {
			return DOT_DASH_BLANK.matcher(object).replaceAll(StringUtils.EMPTY);
		}
	};

	/**
	 * Supression des blancs, points et autres tirets
	 */
	public static final StringRenderer<String> IDE_RENDERER = new AbstractStringRendererImpl<String>() {
		@Override
		protected String toStringFromNotNull(@NotNull String object) {
			return DOT_DASH_BLANK.matcher(object).replaceAll(StringUtils.EMPTY);
		}
	};

	/**
	 * Supression des blancs, points et autres tirets
	 */
	public static final StringRenderer<String> NUM_RC_RENDERER = new AbstractStringRendererImpl<String>() {
		@Override
		protected String toStringFromNotNull(@NotNull String object) {
			return DOT_DASH_BLANK.matcher(object).replaceAll(StringUtils.EMPTY);
		}
	};

	/**
	 * Utilisation de {@link Constants#OUI} et {@link Constants#NON}
	 */
	public static final StringRenderer<Boolean> BOOLEAN_RENDERER = new AbstractStringRendererImpl<Boolean>() {
		@Override
		protected String toStringFromNotNull(@NotNull Boolean object) {
			return booleanToString(object.booleanValue());
		}
	};

	public static final StringRenderer<Collection<RegDate>> STORAGE_REGDATE_COLLECTION_RENDERER = new CollectionStringRenderer<>(STORAGE_REGDATE_RENDERER);

	public static final StringRenderer<Collection<RegDate>> INDEXATION_REGDATE_COLLECTION_RENDERER = new CollectionStringRenderer<>(INDEXATION_REGDATE_RENDERER);

	public static String booleanToString(Boolean value) {
		return BOOLEAN_RENDERER.toString(value);
	}

	public static String booleanToString(boolean value) {
		return value ? Constants.OUI : Constants.NON;
	}

	public static String dateToString(RegDate date, DateStringMode mode) {
		switch (mode) {
			case STORAGE:
				return STORAGE_REGDATE_RENDERER.toString(date);
			case INDEXATION:
				return INDEXATION_REGDATE_RENDERER.toString(date);
			default:
				throw new IllegalArgumentException("Unsupported mode : " + mode);
		}
	}

	public static String dateCollectionToString(Collection<RegDate> collection, DateStringMode mode) {
		switch (mode) {
		case STORAGE:
			return STORAGE_REGDATE_COLLECTION_RENDERER.toString(collection);
		case INDEXATION:
			return INDEXATION_REGDATE_COLLECTION_RENDERER.toString(collection);
		default:
			throw new IllegalArgumentException("Unsupported mode : " + mode);
		}
	}

	public static <T extends Enum<T>> String enumToString(T value) {
		//noinspection unchecked
		return DEFAULT_RENDERER.toString(value);
	}

	public static <T extends Number> String numberToString(T number) {
		//noinspection unchecked
		return DEFAULT_RENDERER.toString(number);
	}

	public static String nullableStringToString(String value) {
		//noinspection unchecked
		return DEFAULT_RENDERER.toString(value);
	}

	public static String noAvsToString(String avs) {
		return AVS_RENDERER.toString(avs);
	}

	public static String noIdeToString(String ide) {
		return IDE_RENDERER.toString(ide);
	}

	public static String numRCToString(String numeroRC) {
		return NUM_RC_RENDERER.toString(numeroRC);
	}

	private static String dateToSearchableString(RegDate date) {
		final int year = date.year();
		final int month = date.month();
		final int day = date.day();
		final List<String> values = new LinkedList<>();

		// la date elle-même sous ses formes "partielles" (pour retrouver cette date par une autre plus partielle que celle-ci)
		values.add(RegDateHelper.toIndexString(RegDate.get(year)));
		if (month != RegDate.UNDEFINED) {
			values.add(RegDateHelper.toIndexString(RegDate.get(year, month)));
		}
		if (day != RegDate.UNDEFINED) {
			values.add(RegDateHelper.toIndexString(date));
		}

		// cas particulier des dates partielles, on doit pouvoir retrouver cette date par une donnée plus précise...
		if (month == RegDate.UNDEFINED) {
			// chaque mois en partiel
			for (RegDate d = RegDate.get(year, 1) ; d.year() == year ; d = d.addMonths(1)) {
				values.add(RegDateHelper.toIndexString(d));
			}

			// chaque jour de l'année
			for (RegDate d = RegDate.get(year, 1, 1) ; d.year() == year ; d = d.getOneDayAfter()) {
				values.add(RegDateHelper.toIndexString(d));
			}
		}
		else if (day == RegDate.UNDEFINED) {
			// chaque jour du mois
			for (RegDate d = RegDate.get(year, month, 1) ; d.month() == month ; d = d.getOneDayAfter()) {
				values.add(RegDateHelper.toIndexString(d));
			}
		}

		// reconstitution de la chaîne de caractères finale
		final StringBuilder b = new StringBuilder();
		for (String value : values) {
			if (b.length() > 0) {
				b.append(' ');
			}
			b.append(value);
		}
		return b.toString();
	}

	public static boolean isBlank(String value) {
		return StringUtils.isBlank(value) || NULL_VALUE.equals(value);
	}

	public static String nullValue() {
		return NULL_VALUE;
	}
}

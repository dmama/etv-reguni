package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.NullableComparator;
import ch.vd.uniregctb.common.NullableDefaultComparator;

/**
 * Classe qui regroupe quelques classes et méthodes utiles dans la comparaison d'invididus civils
 */
public abstract class IndividuComparisonHelper {

	private static final String APPARITION = "apparition";
	private static final String DISPARITION = "disparition";

	public static final Comparator<DateRange> RANGE_COMPARATOR = new NullableComparator<DateRange>(true) {
		@Override
		protected int compareNonNull(@NotNull DateRange o1, @NotNull DateRange o2) {
			return DateRangeComparator.compareRanges(o1, o2);
		}
	};

	public static final Comparator<Integer> INTEGER_COMPARATOR = new NullableDefaultComparator<>(true);

	/**
	 * Interface de vérification d'égalité
	 * @param <T> type de la donnée à vérifier
	 */
	public interface Equalator<T> {
		boolean areEqual(T o1, T o2, @Nullable FieldMonitor monitor, String fieldName);
	}

	/**
	 * Ajoute une donnée dans le moniteur correspondant à l'apparition ou la disparition d'un élément
	 * @param apparition <code>true</code> s'il s'agit d'une apparition, <code>false</code> s'il s'agit d'une disparition
	 * @param monitor le moniteur à compléter
	 * @param fieldName le nom de l'attribut qui apparaît ou disparaît
	 */
	public static void fillMonitorWithApparitionDisparition(boolean apparition, @Nullable FieldMonitor monitor, String fieldName) {
		if (monitor != null) {
			monitor.addField(apparition ? APPARITION : DISPARITION);
			monitor.addField(fieldName);
		}
	}

	/**
	 * Ajoute une donnée dans le moniteur
	 * @param monitor le moniteur à compléter
	 * @param fieldName la donnée à ajouter
	 */
	public static void fillMonitor(@Nullable FieldMonitor monitor, String fieldName) {
		if (monitor != null) {
			monitor.addField(fieldName);
		}
	}

	/**
	 * Vérificateur d'égalité de données qui peuvent être nulles
	 * @param <T> type de la donnée à vérifier
	 */
	public abstract static class NullableEqualator<T> implements Equalator<T> {

		@Override
		public final boolean areEqual(T o1, T o2, @Nullable FieldMonitor monitor, String fieldName) {
			if (o1 == o2) {
				return true;
			}
			else if (o1 == null || o2 == null) {
				fillMonitorWithApparitionDisparition(o1 == null, monitor, fieldName);
				return false;
			}
			else {
				return areNonNullEqual(o1, o2, monitor, fieldName);
			}
		}

		protected abstract boolean areNonNullEqual(@NotNull T o1, @NotNull T o2, @Nullable FieldMonitor monitor, @Nullable String fieldName);
	}

	/**
	 * Vérificateur d'égalité de données qui peuvent être nulles, basé sur l'appel à la méthode {@link Object#equals(Object)}
	 * @param <T> type de la donnée à vérifier
	 */
	public static class DefaultEqualator<T> extends NullableEqualator<T> {
		protected boolean areNonNullEqual(@NotNull T o1, @NotNull T o2, @Nullable FieldMonitor monitor, @Nullable String fieldName) {
			if (o1.equals(o2)) {
				return true;
			}
			fillMonitor(monitor, fieldName);
			return false;
		}
	}

	public static final Equalator<RegDate> DATE_EQUALATOR = new DefaultEqualator<>();

	public static final Equalator<DateRange> RANGE_EQUALATOR = new NullableEqualator<DateRange>() {
		@Override
		protected boolean areNonNullEqual(@NotNull DateRange o1, @NotNull DateRange o2, @Nullable FieldMonitor monitor, @Nullable String fieldName) {
			if (DateRangeHelper.equals(o1, o2)) {
				return true;
			}
			fillMonitor(monitor, fieldName);
			return false;
		}
	};

	public static final Equalator<Integer> INTEGER_EQUALATOR = new DefaultEqualator<>();

	/**
	 * @param c1 collection 1
	 * @param c2 collection 2
	 * @param comparator comparateur utilisé pour trier les collections et s'affranchir de l'ordre des éléments dans les collections
	 * @param equalator vérificateur utilisé pour vérifier l'égalité des objets entre les deux collections
	 * @param <T> type de contenu des collections à vérifier
	 * @return <code>true/code> si les deux collections contiennent les mêmes éléments (même dans le désordre)
	 */
	public static <T> boolean areContentsEqual(Collection<T> c1, Collection<T> c2, Comparator<T> comparator, Equalator<T> equalator, @Nullable FieldMonitor monitor, String fieldName) {
		if (c1 == c2) {
			return true;
		}
		else {
			final int size1 = c1 != null ? c1.size() : 0;
			final int size2 = c2 != null ? c2.size() : 0;
			if (size1 != size2) {
				fillMonitorWithApparitionDisparition(size1 < size2, monitor, fieldName);
				return false;
			}
			else {
				final List<T> sl1 = new ArrayList<>(c1 != null ? c1 : Collections.emptyList());
				Collections.sort(sl1, comparator);

				final List<T> sl2 = new ArrayList<>(c2 != null ? c2 : Collections.emptyList());
				Collections.sort(sl2, comparator);

				for (int i = 0 ; i < sl1.size() ; ++ i) {
					final T o1 = sl1.get(i);
					final T o2 = sl2.get(i);
					if (o1 != o2 && (o1 == null || o2 == null)) {
						fillMonitorWithApparitionDisparition(o1 == null, monitor, fieldName);
						return false;
					}
					else if (!equalator.areEqual(o1, o2, monitor, fieldName)) {
						return false;
					}
				}
				return true;
			}
		}
	}

	/**
	 * Classe de maintenance des champs (et de leurs sous-champs) modifiés
	 */
	public static class FieldMonitor {
		private final List<String> fields = new LinkedList<>();

		/**
		 * Ajoute une indication concernant un champ modifié
		 * @param s le nom du champ (ou l'information à annoncer pour le champ)
		 */
		public void addField(String s) {
			if (StringUtils.isNotBlank(s)) {
				fields.add(0, s);
			}
		}

		/**
		 * La liste des champs collectés. Ils sont dans l'ordre inverse de leur ordre d'insertion (= appels à {@link #addField(String)}).
		 */
		public List<String> getCollectedFields() {
			return fields;
		}

		/**
		 * @return <code>true</code> si aucun élément n'a été collecté
		 */
		public boolean isEmpty() {
			return fields.isEmpty();
		}
	}

	/**
	 * Concaténation des éléments présents dans le moniteur fourni
	 * @param monitor collecteur d'éléments à utiliser
	 * @return la chaîne concaténée
	 */
	@Nullable
	public static String buildErrorMessage(FieldMonitor monitor) {
		if (monitor.isEmpty()) {
			return null;
		}
		else {
			final StringBuilder b = new StringBuilder();
			final List<String> list = monitor.getCollectedFields();
			final Iterator<String> iter = list.iterator();
			while (iter.hasNext()) {
				final String elt = iter.next();
				b.append(elt);
				if (iter.hasNext()) {
					b.append(" (");
				}
			}
			if (list.size() > 1) {
				for (int i = 0 ; i < list.size() - 1 ; ++ i) {
					b.append(")");
				}
			}
			return b.toString();
		}
	}
}

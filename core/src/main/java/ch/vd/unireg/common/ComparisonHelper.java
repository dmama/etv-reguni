package ch.vd.unireg.common;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

public abstract class ComparisonHelper {

	/**
	 * Méthode utilitaire générique pour comparer deux objets potentiellement nullables
	 * @param one un premier objet
	 * @param other un autre objet
	 * @param <T> type des objets à comparer
	 * @return <code>true</code> si les deux objets sont soit tous deux nulls, soit identiques, soit égaux (au sens de {@link Object#equals(Object)})
	 */
	public static <T> boolean areEqual(@Nullable T one, @Nullable T other) {
		return one == other || (one != null && one.equals(other));
	}

	/**
	 * Méthode utilitaire générique pour comparer deux nombres potentiellement nullables
	 * @param one un premier nombre
	 * @param other un autre nombre
	 * @return <code>true</code> si les deux nombres sont soit tous deux nulls, soit identiques, soit égaux (au sens de {@link Comparable#compareTo(Object)})
	 */
	public static boolean areEqual(@Nullable BigDecimal one, @Nullable BigDecimal other) {
		//noinspection NumberEquality
		return one == other || (one != null && other != null && one.compareTo(other) == 0);
	}

	/**
	 * Méthode utilitaire pour comparer deux valeurs d'une même énumération
	 * @param one une valeur
	 * @param other l'autre valeur
	 * @param <E> le type d'énum
	 * @return <code>true</code> si les deux valeurs sont identiques
	 */
	public static <E extends Enum<E>> boolean areEqual(@Nullable E one, @Nullable E other) {
		return one == other;
	}

	/**
	 * Méthode utilitaire pour comparer deux valeurs de dates
	 * @param one une valeur
	 * @param other l'autre valeur
	 * @return <code>true</code> si les deux valeurs sont identiques
	 */
	public static boolean areEqual(@Nullable RegDate one, @Nullable RegDate other) {
		return one == other;
	}
}

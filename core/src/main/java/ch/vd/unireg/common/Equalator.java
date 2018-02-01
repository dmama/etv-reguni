package ch.vd.uniregctb.common;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Interface qui correspond à un prédicat qui prend deux objets de même type et indique s'ils doivent être considérés comme égaux
 * @param <T> le types des objets à comparer
 */
@FunctionalInterface
public interface Equalator<T> extends BiPredicate<T, T> {

	/**
	 * Equalator qui se base sur le résultat de la méthode {@link Object#equals(Object)} pour deux objets non-nulls.
	 * Si l'un des deux objets est null, les deux doivent l'être pour que l'égalité soit déclarée.
	 */
	Equalator<Object> DEFAULT = Objects::equals;
}

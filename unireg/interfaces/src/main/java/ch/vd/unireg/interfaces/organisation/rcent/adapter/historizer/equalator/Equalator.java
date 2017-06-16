package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface Equalator<T> extends BiPredicate<T, T> {

	/**
	 * Equalator qui se base sur le résultat de la méthode {@link Object#equals(Object)} pour deux objets non-nulls.
	 * Si l'un des deux objets est null, les deux doivent l'être pour que l'égalité soit déclarée.
	 */
	Equalator<Object> DEFAULT = (u, v) -> u == v || (u != null && v != null && u.equals(v));

}

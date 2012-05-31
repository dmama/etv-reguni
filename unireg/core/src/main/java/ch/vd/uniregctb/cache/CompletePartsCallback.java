package ch.vd.uniregctb.cache;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

public interface CompletePartsCallback<T, P> {

	/**
	 * Cette méthode est appelée par la classe {@link CacheValueWithParts} pour compléter la valeur actuellement cachée avec certaines parties.
	 *
	 * @param delta les parties qui doivent être renseignées sur la valeur retournée.
	 * @return une valeur avec les parties qui vont bien.
	 */
	@NotNull
	T getDeltaValue(Set<P> delta);

	/**
	 * Cette méthode est appelée immédiatement après l'appel à {@link #getDeltaValue(java.util.Set)}. Elle permet au callback de faire - si nécessaire - les traitements voulus sur la valeur complétée.
	 */
	void postCompletion();
}

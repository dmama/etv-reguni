package ch.vd.uniregctb.cache;

import java.util.Set;

public interface CompletePartsCallbackWithException<T, P> {
	/**
	 * Cette méthode est appelée par la classe {@link CacheValueWithParts} pour compléter la valeur actuellement cachée avec certaines parties.
	 *
	 * @param delta les parties qui doivent être renseignées sur la valeur retournée.
	 * @return une valeur avec les parties qui vont bien.
	 * @throws Exception en cas d'erreur dans la récupération des parties demandées.
	 */
	T getDeltaValue(Set<P> delta) throws Exception;
}

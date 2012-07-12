package ch.vd.uniregctb.extraction;

import ch.vd.uniregctb.common.BatchResults;

/**
 * Interface implémentée par les extracteurs qui supportent un traitement par lots parallélisé
 * @param <E> classe des éléments qui composent les lots en entrée
 * @param <R> classe du container des résultats
 */
public interface BatchableParallelExtractor<E, R extends BatchResults<E, R>> extends BatchableExtractor<E, R> {

	/**
	 * @return le niveau de parallélisation souhaité
	 */
	int getNbThreads();
}

package ch.vd.uniregctb.indexer;


/**
 * Interface des éléments pouvant être indexés par Lucene.
 * 
 */
public interface Indexable {

	/**
	 * @return Une structure de données (préférablement de taille fixe -> perfs d'optimisation) qui représentent l'objet devant être indexé.
	 */
	IndexableData getIndexableData();
}

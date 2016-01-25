package ch.vd.uniregctb.migration.pm.indexeur;

public interface SearchParameter<T> {

	/**
	 * @return <code>true</code> si les paramètres n'imposent aucune contrainte
	 */
	boolean isEmpty();
}

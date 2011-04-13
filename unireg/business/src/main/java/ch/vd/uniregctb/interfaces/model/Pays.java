package ch.vd.uniregctb.interfaces.model;

public interface Pays extends EntiteOFS {

	/**
	 * @return <b>vrai</b> si le pays courant est la Suisse, <b>faux</b> autrement.
	 */
	boolean isSuisse();

	/**
	 * @return <b>vrai</b> si le pays existe actuellement; <b>faux</b> si le pays n'existe plus (par exempple, l'URSS).
	 */
	boolean isValide();

	/**
	 * @return <b>vrai</b> si le pays est un état souverain, <b>faux</b> s'il n'est qu'un territoire
	 */
	boolean isEtatSouverain();
}

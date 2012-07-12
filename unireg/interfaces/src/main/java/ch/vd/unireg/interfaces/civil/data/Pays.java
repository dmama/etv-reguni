package ch.vd.unireg.interfaces.civil.data;

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

	/**
	 * @return le numéro OFS de l'état souverain de ce pays (est différent de la valeur renvoyée par {@link #getNoOFS()}
	 * dans le cas des simples territoires)
	 */
	int getNoOfsEtatSouverain();

	/**
	 * @return le code ISO du pays sur deux positions.
	 */
	String getCodeIso2();

	/**
	 * @return le code ISO du pays sur trois positions.
	 */
	String getCodeIso3();
}

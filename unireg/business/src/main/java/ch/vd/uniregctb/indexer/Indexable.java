package ch.vd.uniregctb.indexer;


/**
 * Element indexable par le moteur de recherche.
 * 
 */
public interface Indexable extends SubIndexable {

	/**
	 * @return l'identifiant technique de l'objet à indexer.
	 */
	public Long getID();

	/**
	 * @return le type de l'objet à indexer, généralement sa classe parente,
	 *         mais celà peut-être autre chose.
	 */
	public String getType();
	
	/**
	 * @return L'entité lui même de la classe fille
	 */
	public String getSubType();
	
}

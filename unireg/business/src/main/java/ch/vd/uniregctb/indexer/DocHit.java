package ch.vd.uniregctb.indexer;

/**
 * Structure qui contient le numéro d'un document lucene trouvé suite à une recherche, ainsi que son score.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DocHit {

	/**
	 * Le numéro du document lucene.
	 */
	public final int doc;
	/**
	 * Le score du document par rapport à la recherche ayant produit ce hit.
	 */
	public final float score;

	public DocHit(int doc, float score) {
		this.doc = doc;
		this.score = score;
	}
}

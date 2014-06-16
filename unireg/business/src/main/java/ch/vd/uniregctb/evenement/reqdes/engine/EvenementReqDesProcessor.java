package ch.vd.uniregctb.evenement.reqdes.engine;

/**
 * Interface externe du processeur des événements eReqDes
 */
public interface EvenementReqDesProcessor {

	/**
	 * Demande le traitement asynchrone de l'unité de traitement identifiée par son ID technique
	 * @param id ID technique de l'unité de traitement à lancer
	 */
	void postUniteTraitement(long id);
}

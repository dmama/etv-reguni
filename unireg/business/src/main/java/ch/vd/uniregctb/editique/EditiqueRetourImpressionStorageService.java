package ch.vd.uniregctb.editique;

/**
 * Service qui maintient la donnée des impressions directes éditiques qui reviennent
 * en attendant que le demandeur vienne les chercher
 */
public interface EditiqueRetourImpressionStorageService {

	/**
	 * Appelé à l'arrivée d'une nouvelle impression par le listener de messages éditique
	 * @param resultat représentation du document imprimé
	 */
	void onArriveeRetourImpression(EditiqueResultat resultat);

	/**
	 * Récupère le document identifié par son nom (champ {@link EditiqueHelper#DI_ID} dans l'entête)
	 * @param nomDocument identifiant du document à récupérer
	 * @param timeout temps maximal d'attente de l'arrivée de l'impression, en millisecondes
	 * @return la représentation du document imprimé renvoyé par l'éditique, <code>null</code> si rien n'est revenu dans le temps imparti
	 */
	EditiqueResultat getDocument(String nomDocument, long timeout);
}

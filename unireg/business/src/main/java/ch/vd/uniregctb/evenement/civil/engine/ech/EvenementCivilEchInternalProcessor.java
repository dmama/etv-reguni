package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;

/**
 * Volontairement laissée au niveau package pour un peu plus d'encapsulation
 */
interface EvenementCivilEchInternalProcessor {

	/**
	 * Traitement de l'événement donné
	 *
	 * @param evt descripteur de l'événement civil cible à traiter
	 * @param evts liste des descripteurs d'événements (en cas d'échec sur le traitement de l'événement cible, ceux qui sont après lui dans cette liste seront passés en attente, voir {@link #errorPostProcessing(java.util.List)})
	 * @param pointer indicateur de la position de l'événement civil cible dans la liste des événements
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'un au moins des événements a terminé en erreur
	 */
	boolean processEventAndDoPostProcessingOnError(EvenementCivilEchBasicInfo evt, List<EvenementCivilEchBasicInfo> evts, int pointer);
}
package ch.vd.uniregctb.evenement.engine;

import ch.vd.uniregctb.common.StatusManager;


/**
 * Moteur de régles pour : <ul> <li> la validation des événements civils</li> <li> le traitement des événements civils</li> </ul>
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 */
public interface EvenementCivilProcessor {

	/**
	 * Lance la validation des événements civils dont le status n'est pas TRAITE. appelé uniquement par le batch
	 *
	 * @param status un status manager (optionel, peut être nul)
	 */
	public void traiteEvenementsCivils(StatusManager status);


	/**
	 * Traite un événement civil désigné par l'id et recycle tous les événements de l'individu
	 *
	 * @param id l'id de l'événement civil
	 * @return 0 dans tous les cas
	 */
	public Long traiteEvenementCivil(Long id);

	/**
	 * Traite un événement civil désigné par l'id et recycle les événements en erreur de l'individu
	 *
	 * @param id l'id de l'événement civil
	 * @return 0 dans tous les cas
	 */
	public Long recycleEvenementCivil(Long id);

}

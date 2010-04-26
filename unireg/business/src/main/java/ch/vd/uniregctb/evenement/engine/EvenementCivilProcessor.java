package ch.vd.uniregctb.evenement.engine;

import ch.vd.uniregctb.common.StatusManager;


/**
 * Moteur de régles pour : <ul> <li> la validation des événements civils regroupés</li> <li> le traitement des événements civils regroupés</li> </ul>
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 */
public interface EvenementCivilProcessor {

	/**
	 * Lance la validation des événements civils regroupés dont le status n'est pas TRAITE. appelé uniquement par le batch
	 *
	 * @param status un status manager (optionel, peut être nul)
	 */
	public void traiteEvenementsCivilsRegroupes(StatusManager status);


	/**
	 * Traite un evenement civil regroupe designe par l'id et recycle tous les evt de l'individu
	 *
	 * @param id l'id de l'événement regroupé
	 * @return 0 dans tous les cas
	 */
	public Long traiteEvenementCivilRegroupe(Long id);

	/**
	 * Traite un evenement civil regroupe designe par l'id et recycle les evt en erreur de l'individu
	 *
	 * @param id l'id de l'événement regroupé
	 * @return 0 dans tous les cas
	 */
	public Long recycleEvenementCivilRegroupe(Long id);

}

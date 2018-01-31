package ch.vd.uniregctb.evenement.civil.engine.regpp;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;


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
	void traiteEvenementsCivils(@Nullable StatusManager status);


	/**
	 * Traite un événement civil désigné par l'id et recycle tous les événements de l'individu
	 * @param id l'id de l'événement civil
	 *
	 */
	void traiteEvenementCivil(Long id);

	/**
	 * Traite un événement civil désigné par l'id et recycle les événements en erreur de l'individu
	 * @param id l'id de l'événement civil
	 */
	void recycleEvenementCivil(Long id);

	/**
	 * Place un événement civil dans l'état forcé et rafraîchit le cache civil des individus concernés
	 * @param evenementCivilExterne événement civil à forcer
	 */
	void forceEvenementCivil(EvenementCivilRegPP evenementCivilExterne);
}

package ch.vd.uniregctb.evenement.civil.engine;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Cette interface expose les méthodes qui permette de traduire des événements civils externes en événements civils internes.
 */
public interface EvenementCivilTranslator {

	/**
	 * Traduit un événement civil externe (qui nous vient du registre civil) en un événement civil interne (qui contient tout le comportement métier qui va bien).
	 *
	 * @param event   un événement civil externe
	 * @param options les options d'exécution de l'événement
	 * @return l'événement civil interne correspondant.
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException
	 *          en cas de problème
	 */
	EvenementCivilInterne toInterne(EvenementCivilExterne event, EvenementCivilOptions options) throws EvenementCivilException;
}

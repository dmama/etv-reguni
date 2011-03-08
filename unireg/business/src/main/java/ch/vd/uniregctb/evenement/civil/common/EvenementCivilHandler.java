package ch.vd.uniregctb.evenement.civil.common;

import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;

public interface EvenementCivilHandler {

	/**
	 * Crée une Adapter valide pour ce Handler
	 *
	 * @param event
	 * @param context le context d'exécution de l'événement civil
	 * @return un événement civil interne qui corresponds à l'événement civil externe reçu
	 */
	EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException;
}

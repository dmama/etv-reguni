package ch.vd.uniregctb.evenement.civil.engine.externe;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

public interface EvenementCivilTranslationStrategy {

	/**
	 * Crée un événement civil interne à partir d'un événement civil externe.
	 *
	 *
	 * @param event   un événement civil externe
	 * @param context le context d'exécution de l'événement civil
	 * @param options
	 * @return un événement civil interne qui corresponds à l'événement civil externe reçu
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException
	 *          en cas de problème
	 */
	EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException;
}

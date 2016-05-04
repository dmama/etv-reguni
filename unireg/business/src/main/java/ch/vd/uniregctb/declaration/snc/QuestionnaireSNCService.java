package ch.vd.uniregctb.declaration.snc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;

/**
 * Service autour des questionnaires SNC : génération de tâches en masse, émissions, rappels...
 */
public interface QuestionnaireSNCService {

	/**
	 * @param periodeFiscale la période fiscale à considérer pour la génération des tâches
	 * @param dateTraitement la date de traitement (doit être postérieure à la fin de la période fiscale)
	 * @param nbThreads nombre de threads pour le processing
	 * @param statusManager status manager
	 * @return un résumé des actions accomplies
	 */
	DeterminationQuestionnairesSNCResults determineQuestionnairesAEmettre(int periodeFiscale, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException;
}

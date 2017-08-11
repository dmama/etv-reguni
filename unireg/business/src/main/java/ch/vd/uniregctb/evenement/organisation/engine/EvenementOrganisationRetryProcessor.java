package ch.vd.uniregctb.evenement.organisation.engine;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Interface du service utilisé par le batch de relance des événements organisation
 */
public interface EvenementOrganisationRetryProcessor {

	/**
	 * Relance les événements organisation en attente ou en erreur
	 * @param status status utilisable pour les feedbacks d'avancement
	 */
	void retraiteEvenements(@Nullable StatusManager status);
}

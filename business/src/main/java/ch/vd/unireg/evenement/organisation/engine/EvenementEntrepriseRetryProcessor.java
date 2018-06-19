package ch.vd.unireg.evenement.organisation.engine;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.StatusManager;

/**
 * Interface du service utilisé par le batch de relance des événements entreprise
 */
public interface EvenementEntrepriseRetryProcessor {

	/**
	 * Relance les événements entreprise en attente ou en erreur
	 * @param status status utilisable pour les feedbacks d'avancement
	 */
	void retraiteEvenements(@Nullable StatusManager status);
}

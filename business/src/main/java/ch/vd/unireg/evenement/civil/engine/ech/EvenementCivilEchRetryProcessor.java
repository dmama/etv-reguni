package ch.vd.unireg.evenement.civil.engine.ech;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.StatusManager;

/**
 * Interface du service utilisé par le batch de relance des événements civils eCH
 */
public interface EvenementCivilEchRetryProcessor {

	/**
	 * Relance les événements civils eCH en attente ou en erreur
	 * @param status status utilisable pour les feedbacks d'avancement
	 */
	void retraiteEvenements(@Nullable StatusManager status);
}

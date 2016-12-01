package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;

/**
 * Interface commune aux processeurs de traitement des mutations du registre foncier.
 */
public interface MutationRFProcessor {
	/**
	 * Traite la mutation passée en paramètre. L'appelant de cette méthode doit gérer lui-même l'ouverture de la transaction.
	 *
	 * @param mutation la mutation à traiter.
	 */
	void process(@NotNull EvenementRFMutation mutation);
}

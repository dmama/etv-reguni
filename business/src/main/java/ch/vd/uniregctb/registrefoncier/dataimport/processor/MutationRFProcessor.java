package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;

/**
 * Interface commune aux processeurs de traitement des mutations du registre foncier.
 */
public interface MutationRFProcessor {
	/**
	 * Traite la mutation passée en paramètre. L'appelant de cette méthode doit gérer lui-même l'ouverture de la transaction.
	 *
	 * @param mutation      la mutation à traiter.
	 * @param importInitial vrai s'il s'agit de l'import initial
	 * @param rapport       le rapport de traitement
	 */
	void process(@NotNull EvenementRFMutation mutation, boolean importInitial, @Nullable MutationsRFProcessorResults rapport);
}

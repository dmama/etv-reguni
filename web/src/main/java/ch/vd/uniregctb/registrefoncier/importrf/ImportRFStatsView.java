package ch.vd.uniregctb.registrefoncier.importrf;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;

/**
 * Statistiques sur l'import des données du registre foncier.
 */
@SuppressWarnings("unused")
public class ImportRFStatsView {

	private final int totalMutations;
	private final int mutationsATraiter;
	private final int mutationsTraitees;
	private final int mutationsEnErreur;
	private final int mutationsForcees;

	public ImportRFStatsView(@NotNull Map<EtatEvenementRF, Integer> countByState) {
		this.mutationsATraiter = countByState.getOrDefault(EtatEvenementRF.A_TRAITER, 0);
		this.mutationsTraitees = countByState.getOrDefault(EtatEvenementRF.TRAITE, 0);
		this.mutationsEnErreur = countByState.getOrDefault(EtatEvenementRF.EN_ERREUR, 0);
		this.mutationsForcees = countByState.getOrDefault(EtatEvenementRF.FORCE, 0);
		this.totalMutations = this.mutationsATraiter + this.mutationsTraitees + this.mutationsEnErreur + this.mutationsForcees;
	}

	public int getTotalMutations() {
		return totalMutations;
	}

	public int getMutationsATraiter() {
		return mutationsATraiter;
	}

	public int getMutationsTraitees() {
		return mutationsTraitees;
	}

	public int getMutationsEnErreur() {
		return mutationsEnErreur;
	}

	public int getMutationsForcees() {
		return mutationsForcees;
	}
}

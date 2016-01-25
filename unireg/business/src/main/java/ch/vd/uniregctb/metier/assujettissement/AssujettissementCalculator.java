package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForsParType;

/**
 * Interface de base des calculateurs d'assujettissement par type de contribuable
 * @param <T> type de contribuable
 */
public interface AssujettissementCalculator<T extends Contribuable> {

	/**
	 * @param ctb le contribuable concerné
	 * @param fpt les fors valides du contribuable triés par type
	 * @param noOfsCommunesVaudoises les numéros OFS des communes vaudoises pour lesquelles on veut spécifiquement calculer l'assujettissement
	 * @return La liste des assujettissements du contribuable, ou <code>null</code> s'il n'en a pas
	 */
	List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException;
}

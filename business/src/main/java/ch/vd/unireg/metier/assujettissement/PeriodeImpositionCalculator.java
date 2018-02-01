package ch.vd.unireg.metier.assujettissement;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.Contribuable;

/**
 * Interface de base des calculateurs de période d'imposition par type de contribuable
 * @param <C> le type de contribuable
 */
public interface PeriodeImpositionCalculator<C extends Contribuable> {

	/**
	 * Calcul de toutes les périodes d'imposition du contribuable donné dont l'assujettissement est donné
	 * @param contribuable contribuable cible
	 * @param assujettissements assujettissements calculés par ailleurs
	 * @return la liste des périodes d'imposition du contribuable correspondant aux assujettissements donnés
	 * @throws AssujettissementException en cas de problème lors du calcul
	 */
	@NotNull
	List<PeriodeImposition> determine(C contribuable, List<Assujettissement> assujettissements) throws AssujettissementException;

}

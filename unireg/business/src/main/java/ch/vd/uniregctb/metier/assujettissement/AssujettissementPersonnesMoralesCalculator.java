package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ForsParType;

public class AssujettissementPersonnesMoralesCalculator implements AssujettissementCalculator<ContribuableImpositionPersonnesMorales> {

	/**
	 * Calcul de l'assujettissement d'une personne morale d'après ses fors
	 * @param ctb contribuable PM
	 * @param fpt liste des fors triés et classés de la PM
	 * @param noOfsCommunesVaudoises (optionnelle) liste des numéros OFS des communes vaudoises pour lesquelles on veut spécifiquement calculer l'assujettissement
	 * @return la liste des assujettissemnts calculés, ou <code>null</code> si le contribuable n'est pas assujetti du tout
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement du contribuable.
	 */
	@Override
	public List<Assujettissement> determine(ContribuableImpositionPersonnesMorales ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		// TODO à implémenter...
		return null;
	}
}

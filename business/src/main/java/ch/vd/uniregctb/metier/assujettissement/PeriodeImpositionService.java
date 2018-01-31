package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;

public interface PeriodeImpositionService {

	/**
	 * Méthode générale de calcul des périodes d'imposition d'un contribuable quelconque
	 * @param contribuable le contribuable
	 * @return les périodes d'imposition calculées
	 * @throws AssujettissementException en cas de problème au calcul de l'assujettissement
	 */
	List<PeriodeImposition> determine(Contribuable contribuable) throws AssujettissementException;

	List<PeriodeImposition> determine(Contribuable contribuable, int periodeFiscale) throws AssujettissementException;

	/**
	 * Détermine la liste des périodes d'imposition durant (= intersectant) la période spécifiée.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param range        la période considérée
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws ch.vd.uniregctb.metier.assujettissement.AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	List<PeriodeImposition> determine(ContribuableImpositionPersonnesPhysiques contribuable, @Nullable DateRange range) throws AssujettissementException;

	/**
	 * Détermine la période d'imposition pour un assujettissement particulier.
	 *
	 * @param fors             la décomposition des fors duquel est issu l'assujettissement
	 * @param assujettissement l'assujettissement dont on veut déterminer la période d'imposition
	 * @return une période d'imposition; ou <b>null</b> si le contribuable ne reçoit pas de déclaration d'impôt.
	 */
	PeriodeImpositionPersonnesPhysiques determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement);
}

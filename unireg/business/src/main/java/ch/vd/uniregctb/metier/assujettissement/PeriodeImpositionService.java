package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.tiers.Contribuable;

public interface PeriodeImpositionService {
	/**
	 * Détermine la liste des périodes d'imposition durant l'année spécifiée.
	 * <p/>
	 * Cette méthode appelle la méthode {@link ch.vd.uniregctb.metier.assujettissement.AssujettissementService#determine(ch.vd.uniregctb.tiers.Contribuable, int)} et applique les règles métier pour en déduire les périodes d'imposition.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param annee        l'année correspondant à la période fiscale considérée (du 1er janvier au 31 décembre)
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws ch.vd.uniregctb.metier.assujettissement.AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	List<PeriodeImposition> determine(Contribuable contribuable, int annee) throws AssujettissementException;

	/**
	 * Détermine la liste des périodes d'imposition durant la période spécifiée.
	 * <p/>
	 * Cette méthode fonctionne en calculant les périodes d'imposition année après année et en ajoutant les résultats l'un après l'autre. Elle n'est donc pas terriblement efficace, et dans la mesure du
	 * possible préférer la méthode {@link #determine(ch.vd.uniregctb.tiers.Contribuable, int)}.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param range        la période considérée
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws ch.vd.uniregctb.metier.assujettissement.AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	List<PeriodeImposition> determine(Contribuable contribuable, @Nullable DateRange range) throws AssujettissementException;

	/**
	 * Détermine la liste des périodes d'imposition durant l'année spécifiée.
	 * <p/>
	 * Cette méthode appelle la méthode {@link ch.vd.uniregctb.metier.assujettissement.AssujettissementService#determine(ch.vd.uniregctb.tiers.Contribuable, int)} et applique les règles métier pour en déduire les périodes d'imposition.
	 *
	 * @param fors la décomposition des fors précalculée par l'année considérée
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws ch.vd.uniregctb.metier.assujettissement.AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	List<PeriodeImposition> determine(DecompositionForsAnneeComplete fors) throws AssujettissementException;

	/**
	 * Détermine la période d'imposition pour un assujettissement particulier.
	 *
	 * @param fors             la décomposition des fors duquel est issu l'assujettissement
	 * @param assujettissement l'assujettissement dont on veut déterminer la période d'imposition
	 * @return une période d'imposition; ou <b>null</b> si le contribuable ne reçoit pas de déclaration d'impôt.
	 */
	PeriodeImposition determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement);
}

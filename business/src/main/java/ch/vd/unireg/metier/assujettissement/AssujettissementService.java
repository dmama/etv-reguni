package ch.vd.unireg.metier.assujettissement;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;

public interface AssujettissementService {

	/**
	 * Analyse les fors du contribuable et construit la liste complète des périodes d'assujettissement.
	 *
	 * @param ctb le contribuable dont on veut déterminer l'assujettissement
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<Assujettissement> determine(Contribuable ctb) throws AssujettissementException;

	/**
	 * Analyse les fors du contribuable PP et construit la liste complète des périodes d'assujettissement <i>du point de vue du rôle</i>.
	 *
	 * @param ctb le contribuable dont on veut déterminer l'assujettissement
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<Assujettissement> determineRole(ContribuableImpositionPersonnesPhysiques ctb) throws AssujettissementException;

	/**
	 * Analyse les fors du contribuable PP et construit la liste complète des périodes d'assujettissement <i>du point de vue de la source</i>.
	 *
	 * @param ctb le contribuable dont on veut déterminer l'assujettissement
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<SourcierPur> determineSource(ContribuableImpositionPersonnesPhysiques ctb) throws AssujettissementException;

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement complète du point de vue des communes vaudoises dont les numéros OFS sont donnés en paramètre
	 * <p/><p/> <b>ATTENTION:</b> cette méthode n'est pas capable de faire la différence entre un vaudois avec for secondaire sur une commune (celle donnée en paramètre) différente de la commune de
	 * domicile et un hors-canton qui a le même for secondaire... (en d'autres termes : l'assujettissement du vaudois vu de la commune où il a son for secondaire sera HorsCanton !!)
	 *
	 * @param ctb                    le contribuable dont on veut déterminer l'assujettissement
	 * @param noOfsCommunesVaudoises les numéros OFS des communes vaudoises dont on veut le point de vue
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<Assujettissement> determinePourCommunes(Contribuable ctb, Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException;

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement durant l'année spécifiée. Dans la grande majorité des cas, il n'y a qu'une seule période d'assujettissement
	 * et elle coïncide avec l'année civile. Dans certains cas rares, il peut y avoir deux - voire même plus que de deux - périodes d'assujettissement distinctes.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param annee        l'année correspondant à la période fiscale considérée (du 1er janvier au 31 décembre)
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<Assujettissement> determine(Contribuable contribuable, int annee) throws AssujettissementException;

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement durant la période spécifiée.
	 * <p/>
	 * Cette méthode fonctionne en calculant l'assujettissement année après année et en collant les résultats l'un après l'autre. Elle n'est donc pas terriblement efficace, et dans la mesure du possible
	 * préférer la méthode {@link #determine(ch.vd.unireg.tiers.Contribuable, int)}.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param range        la période considérée
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<Assujettissement> determine(Contribuable contribuable, @Nullable DateRange range) throws AssujettissementException;

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement découpées suivant les ranges donnés
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param splittingRanges les ranges correspondant aux dates de découpages souhaitées
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	@Nullable
	List<Assujettissement> determine(Contribuable contribuable, List<DateRange> splittingRanges) throws AssujettissementException;
}

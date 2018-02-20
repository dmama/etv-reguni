package ch.vd.unireg.registrefoncier;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.AnnulableHelper;

public class ServitudeCombinator {

	/**
	 * Met à plat les combinaisons bénéficiaire/immeuble d'une servitude donnée.
	 * <p/>
	 * Exemple:
	 * <ul>
	 *     <li>Servitude
	 *         <ol>
	 *             <li>(bénéficiaire 1 + bénéficiaire 2) + (immeuble 1 + immeuble 2)</li>
	 *         </ol>
	 *     </li>
	 *     <li>Résultat
	 *         <ol>
	 *             <li>bénéficiaire 1 + immeuble 1</li>
	 *             <li>bénéficiaire 1 + immeuble 2</li>
	 *             <li>bénéficiaire 2 + immeuble 1</li>
	 *             <li>bénéficiaire 2 + immeuble 2</li>
	 *         </ol>
	 *     </li>
	 * </ul>
	 */
	public static List<ServitudeRF> combinate(@NotNull ServitudeRF source,
	                                         @Nullable Predicate<BeneficeServitudeRF> beneficeFilter,
	                                         @Nullable Predicate<ChargeServitudeRF> chargeFilter) {

		// [SIFISC-28067] une servitude est liée à un ou plusieurs bénéficiaires et un ou plusieurs immeubles.
		// Ces liens sont historisés : ils possèdent des dates de début et de fin qui déterminent une plage de
		// validité. Pour calculer les combinations de servitudes pour chaque ayant-droit et chaque immeuble,
		// il faut donc traverser les liens servitudes -> bénéficiaires et servitudes -> immeubles sans oublier
		// de tenir compte de leurs périodes de validité.
		//
		// Dans le stream ci-dessous, on calcule des servitudes transientes dont les plages de validité ont
		// été adaptées en fonction des périodes de validité des liens. Chaque servitude ainsi calculé pointe
		// vers un bénéficaire et un immeuble pour une période donnée.
		return source
				// combinaisons sur l'axe des bénéficiaires
				.getBenefices().stream()
				.sorted(Comparator.comparing(bene -> bene.getAyantDroit().getIdRF()))
				.filter(AnnulableHelper::nonAnnule)
				.filter(benefice -> beneficeFilter == null || beneficeFilter.test(benefice))
				.map(lien -> reduceForAyantDroit(lien.getServitude(), lien))   // adaption de la période de validité par rapport au lien servitude -> bénéficaire
				.filter(Objects::nonNull)                                      // le stream contient maintenant des servitudes avec un seul bénéficiaire et plusieurs immeubles
				// combinaisons sur l'axe des immeubles
				.map(ServitudeRF::getCharges)
				.flatMap(Collection::stream)
				.sorted(Comparator.comparing(charge -> charge.getImmeuble().getIdRF()))
				.filter(AnnulableHelper::nonAnnule)
				.filter(charge -> chargeFilter == null || chargeFilter.test(charge))
				.map(lien -> reduceForImmeuble(lien.getServitude(), lien))     // adaption de la période de validité par rapport au lien servitude -> immeuble
				.filter(Objects::nonNull)                                      // le stream contient maintenant des servitudes avec un seul bénéficiaire et un seul immeuble
				.collect(Collectors.toList());

	}

	/**
	 * Retourne une copie de la servitude spécifiée dont la période de validité métier est réduite en tenant compte du bénéfice de servitude spéficié
	 * (= intersection de la période de validité métier de la servitude avec la période de validité du bénéfice de servitude).
	 *
	 * @param reference une servitude de référence
	 * @param benefice  le bénéfice de l'ayant-droit sur la servitude
	 * @return la servitude avec sa période de validité métier adaptée; ou <b>null</b> si les deux périodes sont disjointes.
	 */
	@Nullable
	public static ServitudeRF reduceForAyantDroit(@NotNull ServitudeRF reference, @NotNull BeneficeServitudeRF benefice) {

		final DateRange intersection = DateRangeHelper.intersection(reference.getRangeMetier(), benefice);
		if (intersection == null) {
			return null;
		}

		final ServitudeRF adapte = reference.duplicate();
		adapte.setDateDebutMetier(intersection.getDateDebut());
		adapte.setDateFinMetier(intersection.getDateFin());
		// on ne garde que l'ayant-droit
		adapte.getBenefices().removeIf(l -> l.getAyantDroit() != benefice.getAyantDroit());

		if (adapte.getBenefices().isEmpty()) {
			return null;
		}

		return adapte;
	}

	/**
	 * Retourne une copie de la servitude spécifiée dont la période de validité métier est réduite en tenant compte de la charge de servitude spéficiée
	 * (= intersection de la période de validité métier de la servitude avec la période de validité de la charge de servitude).
	 *
	 * @param reference une servitude de référence
	 * @param charge    la charge de servitude de l'immeuble grevé
	 * @return la servitude avec sa période de validité métier adaptée; ou <b>null</b> si les deux périodes sont disjointes.
	 */
	@Nullable
	public static ServitudeRF reduceForImmeuble(@NotNull ServitudeRF reference, @NotNull ChargeServitudeRF charge) {

		final DateRange intersection = DateRangeHelper.intersection(reference.getRangeMetier(), charge);
		if (intersection == null) {
			return null;
		}

		final ServitudeRF adapte = reference.duplicate();
		adapte.setDateDebutMetier(intersection.getDateDebut());
		adapte.setDateFinMetier(intersection.getDateFin());
		// on ne garde que l'immeuble spécifié
		adapte.getCharges().removeIf(c -> c.getImmeuble() != charge.getImmeuble());

		if (adapte.getCharges().isEmpty()) {
			return null;
		}

		return adapte;
	}
}

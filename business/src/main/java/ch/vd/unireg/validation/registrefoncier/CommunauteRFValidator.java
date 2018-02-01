package ch.vd.unireg.validation.registrefoncier;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;
import ch.vd.unireg.validation.EntityValidatorImpl;
import ch.vd.unireg.validation.ValidationService;
import ch.vd.unireg.validation.tiers.TiersValidator;

public class CommunauteRFValidator extends EntityValidatorImpl<CommunauteRF> {

	@Override
	protected Class<CommunauteRF> getValidatedClass() {
		return CommunauteRF.class;
	}

	@Override
	public ValidationResults validate(CommunauteRF entity) {

		final ValidationResults results = new ValidationResults();
		if (entity.isAnnule()){
			return results;
		}

		// on valide les regroupements pour eux-mêmes
		results.merge(validateRegroupements(entity));

		// on vérifie que pour la durée de validité de la communauté, il existe toujours des regroupements
		final Set<DroitProprietePersonneRF> membres = entity.getMembres();
		final Set<RegroupementCommunauteRF> regroupements = entity.getRegroupements();
		if (membres != null || regroupements != null) {

			// on calcule la période de validité de la communauté (une communauté n'a qu'une seule période de validité : il n'y a pas de réutilisation dans le temps)
			final DateRange rangeMembre = determineRangeValidite(membres, HibernateEntity::isAnnule, DroitRF::getRangeMetier);

			// on calcule la période de validité des regroupements (comme la communauté, les regroupements ne devraient avoir qu'une seule période de validité)
			final DateRange rangeRegroupement = determineRangeValidite(regroupements, HibernateEntity::isAnnule, d -> d);

			if (!Objects.equals(rangeMembre, rangeRegroupement)) {
				results.addError("La période de validité des membres de la communauté (" + rangeMembre +
						                 ") ne correspond pas avec la période de validité des regroupements (" + rangeRegroupement + ")");
			}
		}

		return results;
	}

	private ValidationResults validateRegroupements(CommunauteRF modele) {

		final ValidationResults vr = new ValidationResults();

		final Set<RegroupementCommunauteRF> regroupements = modele.getRegroupements();
		if (regroupements != null && !regroupements.isEmpty()) {
			// chaque regroupement pour lui-même
			final ValidationService validationService = getValidationService();
			regroupements.stream()
					.map(validationService::validate)
					.forEach(vr::merge);

			// puis les chevauchements
			if (!vr.hasErrors()) {
				TiersValidator.checkNonOverlap(regroupements,
				                               AnnulableHelper::nonAnnule,
				                               vr,
				                               "regroupements non-annulés");
			}
		}

		return vr;
	}


	@Nullable
	private static <T extends DateRange> DateRange determineRangeValidite(@Nullable Set<T> ranges, @NotNull Function<T, Boolean> annuleGetter, @NotNull Function<T, DateRange> rangeMetierGetter) {

		if (ranges == null || ranges.isEmpty()) {
			return null;
		}

		RegDate dateDebut = RegDateHelper.getLateDate();
		RegDate dateFin = RegDateHelper.getEarlyDate();

		boolean found = false;
		for (T range : ranges) {
			if (annuleGetter.apply(range)) {
				continue;
			}
			found = true;
			final DateRange rangeMetier = rangeMetierGetter.apply(range);
			dateDebut = RegDateHelper.minimum(dateDebut, rangeMetier.getDateDebut(), NullDateBehavior.EARLIEST);
			dateFin = RegDateHelper.maximum(dateFin, rangeMetier.getDateFin(), NullDateBehavior.LATEST);
		}

		if (!found) {
			// tous les ranges sont annulés
			return null;
		}

		return new DateRangeHelper.Range(dateDebut, dateFin);
	}
}

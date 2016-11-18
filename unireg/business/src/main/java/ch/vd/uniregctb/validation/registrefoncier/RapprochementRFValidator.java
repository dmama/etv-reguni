package ch.vd.uniregctb.validation.registrefoncier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

/**
 * Validateur pour les données de rapprochement RF
 */
public class RapprochementRFValidator extends DateRangeEntityValidator<RapprochementRF> {

	private RapprochementRFDAO rapprochementDAO;

	public void setRapprochementDAO(RapprochementRFDAO rapprochementDAO) {
		this.rapprochementDAO = rapprochementDAO;
	}

	@Override
	protected Class<RapprochementRF> getValidatedClass() {
		return RapprochementRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "le rapprochement RF";
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}

	@Override
	public ValidationResults validate(RapprochementRF entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			// le type de rapprochement est obligatoire
			if (entity.getTypeRapprochement() == null) {
				vr.addError(String.format("%s %s ne possède pas de type renseigné", getEntityCategoryName(), getEntityDisplayString(entity)));
			}

			// le lien vers le tiers RF est également obligatoire
			if (entity.getTiersRF() == null) {
				vr.addError(String.format("%s %s n'est attaché à aucun tiers RF", getEntityCategoryName(), getEntityDisplayString(entity)));
			}
			else {
				// un tiers RF ne doit pas avoir plusieurs rapprochements non-annulés qui se chevauchent
				// allons donc y voir !
				final List<RapprochementRF> tous = rapprochementDAO.findByTiersRF(entity.getTiersRF().getId(), true);
				final Map<Long, RapprochementRF> tousParId = Stream.concat(tous.stream(), Stream.of(entity))
						.filter(r -> !r.isAnnule())
						.collect(Collectors.toMap(RapprochementRF::getId,
						                          Function.identity(),
						                          (r1, r2) -> r1));         // on ne conserve que le premier venu avec un identifiant donné
				final List<RapprochementRF> nonAnnulesTries = tousParId.values().stream()
						.sorted(DateRangeComparator::compareRanges)
						.collect(Collectors.toList());

				final List<DateRange> overlaps = DateRangeHelper.overlaps(nonAnnulesTries);
				if (overlaps != null && !overlaps.isEmpty()) {
					overlaps.stream()
							.map(DateRangeHelper::toDisplayString)
							.map(str -> String.format("Le tiers RF %d (numéro RF %d) possède plusieurs rapprochements non-annulés sur la période %s.",
							                          entity.getTiersRF().getId(),
							                          entity.getTiersRF().getNoRF(),
							                          str))
							.forEach(vr::addError);
				}
			}
		}

		return vr;
	}
}

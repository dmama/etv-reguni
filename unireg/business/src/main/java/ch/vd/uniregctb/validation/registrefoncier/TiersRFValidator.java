package ch.vd.uniregctb.validation.registrefoncier;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class TiersRFValidator<T extends TiersRF> extends EntityValidatorImpl<T> {

	@Override
	public ValidationResults validate(T entity) {
		final ValidationResults vr = new ValidationResults();
		vr.merge(validateRapprochements(entity));
		return vr;
	}

	private ValidationResults validateRapprochements(T trf) {

		final ValidationResults vr = new ValidationResults();
		final List<RapprochementRF> rapprochements = trf.getRapprochementsNonAnnulesTries();
		if (!rapprochements.isEmpty()) {
			// à un moment donné, un tiers RF ne doit pas être concerné par plus d'un rapprochement
			final List<DateRange> overlaps = DateRangeHelper.overlaps(rapprochements);
			if (overlaps != null && !overlaps.isEmpty()) {
				overlaps.stream()
						.map(DateRangeHelper::toDisplayString)
						.map(s -> String.format("La période %s est couverte par plusieurs rapprochements non-annulés vers des contribuables", s))
						.forEach(vr::addError);
			}
		}

		return vr;
	}
}

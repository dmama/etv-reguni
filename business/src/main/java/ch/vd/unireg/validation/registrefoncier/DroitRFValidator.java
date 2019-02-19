package ch.vd.unireg.validation.registrefoncier;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

@SuppressWarnings("Duplicates")
public abstract class DroitRFValidator<T extends DroitRF> extends DateRangeEntityValidator<T> {

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T entity) {
		final ValidationResults results = super.validate(entity);

		// une entité annulée est toujours valide...
		if (entity.isAnnule()) {
			return results;
		}

		final RegDate dateDebutMetier = entity.getDateDebutMetier();
		final RegDate dateFinMetier = entity.getDateFinMetier();

		// La date de début métier doit être avant la date de fin métier
		// Si "date de début" = "date de fin", c'est un cas OK (durée d'un jour)
		if (dateDebutMetier != null && dateFinMetier != null && dateDebutMetier.isAfter(dateFinMetier)) {
			results.addError(String.format("%s %s possède une date de début métier qui est après la date de fin métier: début = %s, fin = %s",
			                               getEntityCategoryName(), getEntityDisplayString(entity), RegDateHelper.dateToDisplayString(dateDebutMetier), RegDateHelper.dateToDisplayString(dateFinMetier)));
		}

		return results;
	}
}

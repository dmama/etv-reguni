package ch.vd.unireg.validation.registrefoncier;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.registrefoncier.PrincipalCommunauteRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class PrincipalCommunauteRFValidator extends DateRangeEntityValidator<PrincipalCommunauteRF> {
	@Override
	protected String getEntityCategoryName() {
		return "Le principal du modèle de communauté RF";
	}

	@Override
	protected Class<PrincipalCommunauteRF> getValidatedClass() {
		return PrincipalCommunauteRF.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(PrincipalCommunauteRF entity) {
		final ValidationResults results = super.validate(entity);

		// SIFISC-27135 : La date de début ne doit pas être supérieure à période fiscale N+1
		final RegDate dateDebut = entity.getDateDebut();
		if (dateDebut != null && dateDebut.year() > RegDate.get().addYears(1).year()) {
			results.addError("La date de début ne peut pas dépasser la période fiscale N+1.");
		}

		return results;
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return false;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}

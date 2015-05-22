package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public abstract class DeclarationValidator<T extends Declaration> extends DateRangeEntityValidator<T> {

	@Override
	public ValidationResults validate(T declaration) {
		final ValidationResults vr = super.validate(declaration);
		if (!declaration.isAnnule()) {
			final PeriodeFiscale periode = declaration.getPeriode();
			final RegDate dateDebut = declaration.getDateDebut();
			final RegDate dateFin = declaration.getDateFin();

			if (dateDebut != null && periode != null && dateDebut.year() != periode.getAnnee()) {
				vr.addError(String.format("La date de début [%s] doit correspondre avec l'année de la période [%d].",
				                          RegDateHelper.dateToDisplayString(dateDebut), periode.getAnnee()));
			}

			if (dateFin != null && periode != null && dateFin.year() != periode.getAnnee()) {
				vr.addError(String.format("La date de fin [%s] doit correspondre avec l'année de la période [%d].",
				                          RegDateHelper.dateToDisplayString(dateFin), periode.getAnnee()));
			}
		}
		return vr;
	}

	@Override
	protected boolean isDateOuvertureFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFermetureNullAllowed() {
		return false;
	}

	@Override
	protected boolean isDateFermetureFutureAllowed() {
		return true;
	}
}

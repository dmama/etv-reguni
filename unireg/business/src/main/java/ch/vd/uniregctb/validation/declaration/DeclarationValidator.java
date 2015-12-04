package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class DeclarationValidator<T extends Declaration> extends EntityValidatorImpl<T> {

	@Override
	public ValidationResults validate(T declaration) {

		final ValidationResults vr = new ValidationResults();
		if (!declaration.isAnnule()) {

			ValidationHelper.validate(declaration, false, false, vr);

			final PeriodeFiscale periode = declaration.getPeriode();
			final RegDate dateDebut = declaration.getDateDebut();
			final RegDate dateFin = declaration.getDateFin();

			if (dateDebut == null) {
				vr.addError("La date de début de la déclaration est obligatoire");
			}
			if (dateFin == null) {
				vr.addError("La date de fin de la déclaration est obligatoire");
			}

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
}

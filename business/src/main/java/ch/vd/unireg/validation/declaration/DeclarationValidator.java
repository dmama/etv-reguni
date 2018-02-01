package ch.vd.uniregctb.validation.declaration;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
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

			if (dateDebut != null && periode != null && dateDebut.year() != periode.getAnnee() && isDateDebutForcementDansPeriode()) {
				vr.addError(String.format("La date de début [%s] doit correspondre avec l'année de la période [%d].",
				                          RegDateHelper.dateToDisplayString(dateDebut), periode.getAnnee()));
			}

			if (dateFin != null && periode != null && dateFin.year() != periode.getAnnee() && isDateFinForcementDansPeriode()) {
				vr.addError(String.format("La date de fin [%s] doit correspondre avec l'année de la période [%d].",
				                          RegDateHelper.dateToDisplayString(dateFin), periode.getAnnee()));
			}

			// il faut également valider les états et les délais
			final List<EtatDeclaration> etats = declaration.getEtatsDeclarationSorted();
			if (etats != null) {
				for (EtatDeclaration etat : declaration.getEtatsDeclarationSorted()) {
					vr.merge(getValidationService().validate(etat));
				}
			}
			final List<DelaiDeclaration> delais = declaration.getDelaisDeclarationSorted();
			if (delais != null) {
				for (DelaiDeclaration delai : declaration.getDelaisDeclarationSorted()) {
					vr.merge(getValidationService().validate(delai));
				}
			}
		}
		return vr;
	}

	protected boolean isDateDebutForcementDansPeriode() {
		return true;
	}

	protected boolean isDateFinForcementDansPeriode() {
		return true;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinNullAllowed() {
		return false;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}

package ch.vd.unireg.validation.declaration;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;

public class DeclarationImpotOrdinairePMValidator extends DeclarationImpotOrdinaireValidator<DeclarationImpotOrdinairePM> {

	@Override
	protected Class<DeclarationImpotOrdinairePM> getValidatedClass() {
		return DeclarationImpotOrdinairePM.class;
	}

	@Override
	protected boolean isDateDebutForcementDansPeriode() {
		return false;
	}

	@Override
	public ValidationResults validate(DeclarationImpotOrdinairePM di) {
		final ValidationResults vr = super.validate(di);
		if (!di.isAnnule()) {

			// un exercice commercial doit être référencé
			if (di.getDateDebutExerciceCommercial() == null) {
				vr.addError("La date de début de l'exercice commercial est obligatoire sur une déclaration d'impôt de personne morale.");
			}
			if (di.getDateFinExerciceCommercial() == null) {
				vr.addError("La date de fin de l'exercice commercial est obligatoire sur une déclaration d'impôt de personne morale.");
			}

			// l'exercice commercial doit avoir ses dates dans le bon sens
			if (di.getDateDebutExerciceCommercial() != null && di.getDateFinExerciceCommercial() != null) {
				if (di.getDateDebutExerciceCommercial().isAfter(di.getDateFinExerciceCommercial())) {
					vr.addError("La date de début de l'exercice commercial de la déclaration d'impôt doit être antérieure à sa date de fin.");
				}

				// cet exercice commercial doit inclure la période d'imposition
				final DateRange exerciceCommercial = new DateRangeHelper.Range(di.getDateDebutExerciceCommercial(), di.getDateFinExerciceCommercial());
				if (!DateRangeHelper.within(di, exerciceCommercial)) {
					vr.addError("La période d'imposition d'une déclaration d'impôt doit être comprise dans l'exercice commercial correspondant.");
				}
			}
		}
		return vr;
	}
}

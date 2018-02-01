package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

public abstract class DeclarationImpotOrdinaireValidator<T extends DeclarationImpotOrdinaire> extends DeclarationValidator<T> {

	@Override
	public ValidationResults validate(T di) {
		final ValidationResults vr = super.validate(di);
		if (!di.isAnnule()) {

			if (di.getPeriode() == null) {
				vr.addError("La période ne peut pas être nulle.");
			}

			if (di.getNumero() == null) {
				vr.addError("Le numéro de séquence de la déclaration ne peut pas être nul.");
			}
		}

		return vr;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La DI";
	}
}

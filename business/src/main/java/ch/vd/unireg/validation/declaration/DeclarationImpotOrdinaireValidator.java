package ch.vd.unireg.validation.declaration;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;

public abstract class DeclarationImpotOrdinaireValidator<T extends DeclarationImpotOrdinaire> extends DeclarationValidator<T> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T di) {
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

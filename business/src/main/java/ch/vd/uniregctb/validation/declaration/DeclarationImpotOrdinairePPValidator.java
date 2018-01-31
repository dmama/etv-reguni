package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;

public class DeclarationImpotOrdinairePPValidator extends DeclarationImpotOrdinaireValidator<DeclarationImpotOrdinairePP> {

	@Override
	protected Class<DeclarationImpotOrdinairePP> getValidatedClass() {
		return DeclarationImpotOrdinairePP.class;
	}

	@Override
	public ValidationResults validate(DeclarationImpotOrdinairePP di) {
		final ValidationResults vr = super.validate(di);
		if (!di.isAnnule()) {

			if (di.getModeleDocument() == null) {
				vr.addError("Le modèle de document ne peut pas être nul.");
			}

			if (di.getPeriode() != null
					&& di.getPeriode().getAnnee() >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE
					&& di.getCodeSegment() == null) {
				vr.addError(String.format("Le code segment ne peut être nul pour une déclaration à partir de %d", DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE));
			}
		}

		return vr;
	}
}

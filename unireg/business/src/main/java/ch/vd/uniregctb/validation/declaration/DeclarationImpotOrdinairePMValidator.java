package ch.vd.uniregctb.validation.declaration;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;

public class DeclarationImpotOrdinairePMValidator extends DeclarationImpotOrdinaireValidator<DeclarationImpotOrdinairePM> {

	@Override
	protected Class<DeclarationImpotOrdinairePM> getValidatedClass() {
		return DeclarationImpotOrdinairePM.class;
	}

	@Override
	protected boolean isDateDebutForcementDansPeriode() {
		return false;
	}
}

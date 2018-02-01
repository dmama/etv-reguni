package ch.vd.uniregctb.validation.declaration;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;

public class DeclarationImpotOrdinairePMValidatorTest extends DeclarationImpotOrdinaireValidatorTest<DeclarationImpotOrdinairePM> {

	@Override
	protected String getValidatorBeanName() {
		return "declarationImpotOrdinairePMValidator";
	}

	@Override
	protected DeclarationImpotOrdinairePM newDeclarationInstance() {
		return new DeclarationImpotOrdinairePM();
	}
}
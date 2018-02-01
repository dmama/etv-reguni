package ch.vd.unireg.validation.declaration;

import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;

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
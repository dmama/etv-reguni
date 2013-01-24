package ch.vd.uniregctb.validation.declaration;

import ch.vd.uniregctb.declaration.DeclarationImpotSource;

public class DeclarationImpotSourceValidator extends DeclarationValidator<DeclarationImpotSource> {

	@Override
	protected Class<DeclarationImpotSource> getValidatedClass() {
		return DeclarationImpotSource.class;
	}
}

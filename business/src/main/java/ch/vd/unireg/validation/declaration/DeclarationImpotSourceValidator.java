package ch.vd.unireg.validation.declaration;

import ch.vd.unireg.declaration.DeclarationImpotSource;

public class DeclarationImpotSourceValidator extends DeclarationValidator<DeclarationImpotSource> {

	@Override
	protected Class<DeclarationImpotSource> getValidatedClass() {
		return DeclarationImpotSource.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La LR";
	}
}

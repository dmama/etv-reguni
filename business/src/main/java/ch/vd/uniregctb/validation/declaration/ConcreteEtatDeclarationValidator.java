package ch.vd.uniregctb.validation.declaration;

import ch.vd.uniregctb.declaration.EtatDeclaration;

public class ConcreteEtatDeclarationValidator extends EtatDeclarationValidator<EtatDeclaration> {
	@Override
	protected Class<EtatDeclaration> getValidatedClass() {
		return EtatDeclaration.class;
	}
}

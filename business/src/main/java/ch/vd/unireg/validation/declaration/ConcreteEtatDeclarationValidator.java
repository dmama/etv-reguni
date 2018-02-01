package ch.vd.unireg.validation.declaration;

import ch.vd.unireg.declaration.EtatDeclaration;

public class ConcreteEtatDeclarationValidator extends EtatDeclarationValidator<EtatDeclaration> {
	@Override
	protected Class<EtatDeclaration> getValidatedClass() {
		return EtatDeclaration.class;
	}
}

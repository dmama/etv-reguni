package ch.vd.uniregctb.validation.declaration;

import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.validation.declaration.EtatDeclarationValidator;

public class ConcreteEtatDeclarationValidator extends EtatDeclarationValidator<EtatDeclaration> {
	@Override
	protected Class<EtatDeclaration> getValidatedClass() {
		return EtatDeclaration.class;
	}
}

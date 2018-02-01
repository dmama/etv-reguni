package ch.vd.unireg.efacture;

import ch.vd.unireg.common.DelegatingValidator;

public class EFactureValidator extends DelegatingValidator {

	public EFactureValidator() {
		addSubValidator(ChangeEmailView.class, new ChangeEmailValidator());
		addSubValidator(DestinataireAvecHistoView.class, new DummyValidator<>(DestinataireAvecHistoView.class));
		addSubValidator(ActionDemandeView.class, new DummyValidator<>(ActionDemandeView.class));
		addSubValidator(FreeCommentView.class, new DummyValidator<>(FreeCommentView.class));
	}
}

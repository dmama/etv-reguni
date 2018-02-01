package ch.vd.unireg.validation.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.declaration.EtatDeclarationSommee;

public class EtatDeclarationsommeeValidator extends EtatDeclarationValidator<EtatDeclarationSommee> {
	@Override
	protected Class<EtatDeclarationSommee> getValidatedClass() {
		return EtatDeclarationSommee.class;
	}

	@Override
	public ValidationResults validate(EtatDeclarationSommee ed) {
		final ValidationResults results = super.validate(ed);
		if (!ed.isAnnule()) {
			final RegDate dateEnvoi = ed.getDateEnvoiCourrier();
			if (dateEnvoi == null) {
				final String dateObtention = RegDateHelper.dateToDisplayString(ed.getDateObtention());
				results.addError(String.format("L'etat sommé le %s de la déclaration possède une date d'envoi de courrier nulle", dateObtention));
			}
		}
		return results;
	}
}

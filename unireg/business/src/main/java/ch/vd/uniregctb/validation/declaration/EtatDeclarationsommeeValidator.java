package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;

public class EtatDeclarationsommeeValidator extends EtatDeclarationValidator<EtatDeclarationSommee> {
	@Override
	protected Class<EtatDeclarationSommee> getValidatedClass() {
		return EtatDeclarationSommee.class;
	}

	@Override
	public ValidationResults validate(EtatDeclarationSommee ed) {
		final ValidationResults results = super.validate(ed);
		if (!ed.isAnnule()) {
			RegDate dateEnvoi = ed.getDateEnvoiCourrier();
			if (dateEnvoi == null) {
				String dateObtention= RegDateHelper.dateToDisplayString(ed.getDateObtention());
				results.addError(String.format("L'etat sommé le %s de la déclaration possède une date d'envoi de courrier nulle",dateObtention));
			}
		}
		return results;
	}
}

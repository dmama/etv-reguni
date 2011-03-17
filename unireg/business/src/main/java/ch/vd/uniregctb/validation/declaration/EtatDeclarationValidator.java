package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class EtatDeclarationValidator<T extends EtatDeclaration> extends EntityValidatorImpl<T> {


	public ValidationResults validate(T ed) {

		final ValidationResults results = new ValidationResults();

		if (ed.isAnnule()) {
			return results;
		}

		final RegDate dateObtention = ed.getDateObtention();
		final Declaration declaration = ed.getDeclaration();


		// La date de début doit être renseignée
		if (dateObtention == null) {
			results.addError(String.format("L'etat %s de la déclaration possède une date d'obtention nulle", ed.getEtat().description()));
		}

		return results;
	}
}

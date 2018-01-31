package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;
import ch.vd.uniregctb.validation.documentfiscal.EtatDocumentFiscalValidator;

public abstract class EtatDeclarationValidator<T extends EtatDeclaration> extends EtatDocumentFiscalValidator<T> {


	@Override
	public ValidationResults validate(T ed) {

		final ValidationResults results = new ValidationResults();

		if (ed.isAnnule()) {
			return results;
		}

		final TypeEtatDocumentFiscal etat = ed.getEtat();
		final RegDate dateObtention = ed.getDateObtention();
		final Declaration declaration = ed.getDeclaration();

		if (etat == null) {
			results.addError("L'etat de la déclaration ne possède pas de type d'état");
		}

		// La date de début doit être renseignée
		if (dateObtention == null) {
			results.addError(String.format("L'etat %s de la déclaration possède une date d'obtention nulle", etat == null ? "<de type inconnu>" : etat.descriptionF()));
		}

		return results;
	}
}

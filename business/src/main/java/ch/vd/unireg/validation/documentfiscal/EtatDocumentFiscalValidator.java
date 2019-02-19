package ch.vd.unireg.validation.documentfiscal;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.validation.EntityValidatorImpl;

public abstract class EtatDocumentFiscalValidator<T extends EtatDocumentFiscal> extends EntityValidatorImpl<T> {


	@Override
	@NotNull
	public ValidationResults validate(@NotNull T ed) {

		final ValidationResults results = new ValidationResults();

		if (ed.isAnnule()) {
			return results;
		}

		final TypeEtatDocumentFiscal etat = ed.getEtat();
		final RegDate dateObtention = ed.getDateObtention();

		if (etat == null) {
			results.addError("L'etat du document fiscal ne possède pas de type d'état");
		}

		// La date de début doit être renseignée
		if (dateObtention == null) {
			results.addError(String.format("L'etat %s du document fiscal possède une date d'obtention nulle", etat == null ? "<de type inconnu>" : etat.descriptionF()));
		}

		return results;
	}
}

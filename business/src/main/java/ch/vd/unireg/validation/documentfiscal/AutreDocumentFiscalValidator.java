package ch.vd.unireg.validation.documentfiscal;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.validation.EntityValidatorImpl;

/**
 * Classe de base des validateurs des autres documents fiscaux
 * @param <T> type concret du document fiscal
 */
public abstract class AutreDocumentFiscalValidator<T extends AutreDocumentFiscal> extends EntityValidatorImpl<T> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T entity) {
		final ValidationResults vr = new ValidationResults();
		if (!entity.isAnnule()) {

/* On ne pas plus facilement tester (classe séparée)
			// il doit toujours y avoir une date d'envoi
			if (entity.getDateEnvoi() == null) {
				vr.addError("La date d'envoi d'un document fiscal est obligatoire.");
			}
*/

		}
		return vr;
	}
}

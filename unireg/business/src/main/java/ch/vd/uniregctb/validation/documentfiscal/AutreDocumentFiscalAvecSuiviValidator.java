package ch.vd.uniregctb.validation.documentfiscal;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuivi;

/**
 * Validateur des "autres documents fiscaux" qui sont suivis (= pour lesquels on attend un retour)
 * @param <T> type du document fiscal
 */
public abstract class AutreDocumentFiscalAvecSuiviValidator<T extends AutreDocumentFiscalAvecSuivi> extends AutreDocumentFiscalValidator<T> {

	@Override
	public ValidationResults validate(T entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {

			// un délai de retour doit être inscrit (si on attend un retour, il faut dire jusqu'à quand...)
			if (entity.getDelaiRetour() == null) {
				vr.addError("Le délai de retour est une donnée obligatoire sur un document fiscal suivi.");
			}

		}
		return vr;
	}
}

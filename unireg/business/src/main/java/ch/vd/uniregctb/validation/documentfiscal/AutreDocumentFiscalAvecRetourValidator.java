package ch.vd.uniregctb.validation.documentfiscal;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecRetour;

/**
 * Validateur des "autres documents fiscaux" qui attendent un retour
 * @param <T> type du document fiscal
 */
public abstract class AutreDocumentFiscalAvecRetourValidator<T extends AutreDocumentFiscalAvecRetour> extends AutreDocumentFiscalValidator<T> {

	@Override
	public ValidationResults validate(T entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {

			// un délai de retour doit être inscrit (si on attend un retour, il faut dire jusqu'à quand...)
			if (entity.getDelaiRetour() == null) {
				vr.addError("Le délai de retour est une donnée obligatoire sur un document fiscal qui attend un retour.");
			}

		}
		return vr;
	}
}

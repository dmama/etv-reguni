package ch.vd.uniregctb.validation.documentfiscal;

import java.util.List;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuivi;
import ch.vd.uniregctb.documentfiscal.DelaiDocumentFiscal;
import ch.vd.uniregctb.documentfiscal.EtatDocumentFiscal;

/**
 * Validateur des "autres documents fiscaux" qui sont suivis (= pour lesquels on attend un retour)
 * @param <T> type du document fiscal
 */
public abstract class AutreDocumentFiscalAvecSuiviValidator<T extends AutreDocumentFiscalAvecSuivi> extends AutreDocumentFiscalValidator<T> {

	@Override
	public ValidationResults validate(T autreDocumentFiscal) {
		final ValidationResults vr = super.validate(autreDocumentFiscal);
		if (!autreDocumentFiscal.isAnnule()) {

			// il faut également valider les états et les délais
			final List<EtatDocumentFiscal> etats = autreDocumentFiscal.getEtatsSorted();
			if (etats != null) {
				for (EtatDocumentFiscal etat : autreDocumentFiscal.getEtatsSorted()) {
					vr.merge(getValidationService().validate(etat));
				}
			}
			final List<DelaiDocumentFiscal> delais = autreDocumentFiscal.getDelaisSorted();
			if (delais != null) {
				for (DelaiDocumentFiscal delai : autreDocumentFiscal.getDelaisSorted()) {
					vr.merge(getValidationService().validate(delai));
				}
			}

/* On ne pas plus facilement tester (classe séparée)
			// un délai de retour doit être inscrit (si on attend un retour, il faut dire jusqu'à quand...)
			if (autreDocumentFiscal.getDelaiRetour() == null) {
				vr.addError("Le délai de retour est une donnée obligatoire sur un document fiscal suivi.");
			}
*/
		}
		return vr;
	}
}

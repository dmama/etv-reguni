package ch.vd.unireg.validation.documentfiscal;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.documentfiscal.DelaiAutreDocumentFiscal;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.validation.EntityValidatorImpl;

public class DelaiAutreDocumentFiscalValidator extends EntityValidatorImpl<DelaiAutreDocumentFiscal> {

	@Override
	protected Class<DelaiAutreDocumentFiscal> getValidatedClass() {
		return DelaiAutreDocumentFiscal.class;
	}

	@Override
	public ValidationResults validate(DelaiAutreDocumentFiscal delai) {
		final ValidationResults vr = new ValidationResults();
		if (delai.isAnnule()) {
			return vr;
		}

		if (delai.getDateTraitement() == null) {
			vr.addError("La date de traitement n'est pas renseignée sur le délai du document fiscal.");
		}

		if (delai.getEtat() == null) {
			vr.addError("L'état du délai n'est pas renseigné sur le délai du document fiscal.");
		}
		else {
			if (delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
				if (delai.getDelaiAccordeAu() == null) {
					vr.addError("La date de délai accordé est obligatoire sur un délai dans l'état 'accordé' du document fiscal.");
				}
			}
			else {
				if (delai.getDelaiAccordeAu() != null) {
					vr.addError("La date de délai accordé est interdite sur un délai dans un état différent de 'accordé'.");
				}
				if (delai.isSursis()) {
					vr.addError("Seuls les délais accordés peuvent être dotés du flag 'sursis'.");
				}
			}
		}

		return vr;
	}
}

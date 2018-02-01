package ch.vd.unireg.validation.documentfiscal;

import ch.vd.unireg.documentfiscal.EtatAutreDocumentFiscal;

public class EtatAutreDocumentFiscalValidator extends EtatDocumentFiscalValidator<EtatAutreDocumentFiscal> {

	@Override
	protected Class<EtatAutreDocumentFiscal> getValidatedClass() {
		return EtatAutreDocumentFiscal.class;
	}
}

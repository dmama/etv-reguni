package ch.vd.uniregctb.validation.documentfiscal;

import ch.vd.uniregctb.documentfiscal.EtatAutreDocumentFiscal;

public class EtatAutreDocumentFiscalValidator extends EtatDocumentFiscalValidator<EtatAutreDocumentFiscal> {

	@Override
	protected Class<EtatAutreDocumentFiscal> getValidatedClass() {
		return EtatAutreDocumentFiscal.class;
	}
}

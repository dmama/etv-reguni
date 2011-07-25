package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationReturnCode;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationReturnResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.exception.TaxDeclarationReturnError;

public class TaxDeclarationReturnBuilder {

	public static TaxDeclarationReturnResponse newTaxDeclarationReturnResponse(OrdinaryTaxDeclarationKey key, TaxDeclarationReturnError exception) {
		return new TaxDeclarationReturnResponse(key, exception.getCode(), new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.TAX_DECLARATION_RETURN.value()));
	}

	public static TaxDeclarationReturnResponse newTaxDeclarationReturnResponse(OrdinaryTaxDeclarationKey key, TaxDeclarationReturnCode code) {
		return new TaxDeclarationReturnResponse(key, code, null);
	}

}

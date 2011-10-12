package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationResponse;
import ch.vd.unireg.webservices.party3.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.webservices.party3.TaxDeclarationAcknowledgeCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.exception.TaxDeclarationAcknowledgeError;

public class AcknowledgeTaxDeclarationBuilder {

	public static AcknowledgeTaxDeclarationResponse newAcknowledgeTaxDeclarationResponse(OrdinaryTaxDeclarationKey key, TaxDeclarationAcknowledgeError exception) {
		return new AcknowledgeTaxDeclarationResponse(key, exception.getCode(), new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.TAX_DECLARATION_RETURN.value(), null));
	}

	public static AcknowledgeTaxDeclarationResponse newAcknowledgeTaxDeclarationResponse(OrdinaryTaxDeclarationKey key, TaxDeclarationAcknowledgeCode code) {
		return new AcknowledgeTaxDeclarationResponse(key, code, null);
	}

}

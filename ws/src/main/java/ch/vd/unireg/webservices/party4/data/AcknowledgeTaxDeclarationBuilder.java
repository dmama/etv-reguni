package ch.vd.unireg.webservices.party4.data;

import ch.vd.unireg.webservices.party4.AcknowledgeTaxDeclarationResponse;
import ch.vd.unireg.webservices.party4.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.webservices.party4.TaxDeclarationAcknowledgeCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.webservices.party4.exception.TaxDeclarationAcknowledgeError;

public class AcknowledgeTaxDeclarationBuilder {

	public static AcknowledgeTaxDeclarationResponse newAcknowledgeTaxDeclarationResponse(OrdinaryTaxDeclarationKey key, TaxDeclarationAcknowledgeError exception) {
		return new AcknowledgeTaxDeclarationResponse(key, exception.getCode(), new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.TAX_DECLARATION_RETURN.value(), null));
	}

	public static AcknowledgeTaxDeclarationResponse newAcknowledgeTaxDeclarationResponse(OrdinaryTaxDeclarationKey key, TaxDeclarationAcknowledgeCode code) {
		return new AcknowledgeTaxDeclarationResponse(key, code, null);
	}

}

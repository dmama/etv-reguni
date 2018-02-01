package ch.vd.unireg.webservices.party3.data;

import ch.vd.unireg.webservices.party3.ExtendDeadlineCode;
import ch.vd.unireg.webservices.party3.ExtendDeadlineResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationKey;
import ch.vd.unireg.webservices.party3.exception.ExtendDeadlineError;

public class ExtendDeadlineBuilder {

	public static ExtendDeadlineResponse newExtendDeadlineResponse(TaxDeclarationKey key, ExtendDeadlineError exception) {
		return new ExtendDeadlineResponse(key, exception.getCode(), new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.EXTEND_DEADLINE.value(), null));
	}

	public static ExtendDeadlineResponse newExtendDeadlineResponse(TaxDeclarationKey key, ExtendDeadlineCode code) {
		return new ExtendDeadlineResponse(key, code, null);
	}

}

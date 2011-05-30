package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionCode;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaireKey;
import ch.vd.uniregctb.webservices.tiers3.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.exception.QuittancementErreur;

public class QuittancementBuilder {

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, QuittancementErreur exception) {
		return new ReponseQuittancementDeclaration(key, exception.getCode(), new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.QUITTANCEMENT.value()));
	}

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, CodeQuittancement code) {
		return new ReponseQuittancementDeclaration(key, code, null);
	}

}

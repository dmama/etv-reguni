package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaireKey;
import ch.vd.uniregctb.webservices.tiers3.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.WebServiceExceptionType;
import ch.vd.uniregctb.webservices.tiers3.exception.QuittancementErreur;

public class QuittancementBuilder {
	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, Exception exception, WebServiceExceptionType type) {
		return new ReponseQuittancementDeclaration(key, CodeQuittancement.EXCEPTION, exception.getMessage(), type);
	}

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, QuittancementErreur exception) {
		return new ReponseQuittancementDeclaration(key, exception.getCode(), exception.getMessage(), WebServiceExceptionType.BUSINESS);
	}

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, CodeQuittancement code) {
		return new ReponseQuittancementDeclaration(key, code, null, null);
	}
}

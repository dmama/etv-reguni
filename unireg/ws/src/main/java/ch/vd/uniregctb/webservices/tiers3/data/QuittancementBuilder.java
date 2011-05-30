package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.uniregctb.webservices.tiers3.AccessDeniedExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaireKey;
import ch.vd.uniregctb.webservices.tiers3.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.TechnicalExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.TypeWebServiceException;
import ch.vd.uniregctb.webservices.tiers3.WebServiceExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.exception.QuittancementErreur;

public class QuittancementBuilder {

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, Exception exception, TypeWebServiceException type) {
		return new ReponseQuittancementDeclaration(key, CodeQuittancement.EXCEPTION, buildExceptionInfo(exception, type));
	}

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, QuittancementErreur exception) {
		return new ReponseQuittancementDeclaration(key, exception.getCode(), new BusinessExceptionInfo(exception.getMessage()));
	}

	public static ReponseQuittancementDeclaration newReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, CodeQuittancement code) {
		return new ReponseQuittancementDeclaration(key, code, null);
	}

	private static WebServiceExceptionInfo buildExceptionInfo(Exception exception, TypeWebServiceException type) {
		final WebServiceExceptionInfo info;
		switch (type) {
		case ACCESS_DENIED:
			info = new AccessDeniedExceptionInfo(exception.getMessage());
			break;
		case BUSINESS:
			info = new BusinessExceptionInfo(exception.getMessage());
			break;
		case TECHNICAL:
			info = new TechnicalExceptionInfo(exception.getMessage());
			break;
		default:
			throw new IllegalArgumentException("Type d'exception inconnu = [" + type + "]");
		}
		return info;
	}
}

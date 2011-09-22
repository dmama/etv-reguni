package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;
import ch.vd.uniregctb.webservices.tiers2.impl.exception.QuittancementErreur;

/**
 * Contient les informations de réponse d'une demande de quittancement d'une déclaration d'impôt ordinaire.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>taxDeclarationReturnResponseType</i> (xml) / <i>TaxDeclarationReturnResponse</i> (client java)
 *
 * @see ch.vd.uniregctb.webservices.tiers2.data.DemandeQuittancementDeclaration
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReponseQuittancementDeclaration")
public class ReponseQuittancementDeclaration {

	/**
	 * La clé qui permet d'identifier la déclaration concernée
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>key</i>.
	 */
	@XmlElement(required = true)
	public DeclarationImpotOrdinaireKey key;

	/**
	 * Le code de retour du quittancement.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>code</i>.
	 */
	@XmlElement(required = true)
	public CodeQuittancement code;

	/**
	 * Le message de l'erreur ou de l'exception levée si code != OK
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>exceptionInfo</i>.
	 */
	@XmlElement(required = false)
	public String exceptionMessage;

	/**
	 * Le type de l'erreur ou de l'exception levée si code != OK
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>exceptionInfo</i>.
	 */
	@XmlElement(required = false)
	public WebServiceExceptionType exceptionType;

	public ReponseQuittancementDeclaration() {
	}

	public ReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, CodeQuittancement code) {
		this.key = key;
		this.code = code;
	}

	public ReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, QuittancementErreur e) {
		this.key = key;
		this.code = e.getCode();
		this.exceptionType = e.getType();
		this.exceptionMessage = e.getMessage();
	}

	public ReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, RuntimeException e, WebServiceExceptionType exceptionType) {
		this.key = key;
		this.code = CodeQuittancement.EXCEPTION;
		this.exceptionType = exceptionType;
		this.exceptionMessage = e.getMessage();
	}
}
package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;

/**
 * Contient les informations de réponse d'une demande de quittancement d'une déclaration d'impôt ordinaire.
 *
 * @see ch.vd.uniregctb.webservices.tiers2.data.DemandeQuittancementDeclaration
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReponseQuittancementDeclaration")
public class ReponseQuittancementDeclaration {

//	public enum Code {
//		/**
//		 * La déclaration a bien été quittancée.
//		 */
//		OK,
//		/**
//		 * Le contribuable spécifié est inconnu.
//		 */
//		ERREUR_CTB_INCONNU,
//		/**
//		 * La déclaration spécifiée n'existe pas.
//		 */
//		ERREUR_DECLARATION_INEXISTANTE,
//		/**
//		 * La déclaration d'impôt est déjà quittancée.
//		 */
//		ERREUR_DECLARATION_DEJA_QUITTANCEE,
//		/**
//		 * La déclaration d'impôt est annnulée.
//		 */
//		ERREUR_DECLARATION_ANNULEE,
//		/**
//		 * Une erreur inattendue a eu lieu. Voir le message d'erreur pour plus d'informations.
//		 */
//		EXCEPTION
//	}

	/**
	 * Le code de retour du quittancement, qui permet de déterminer si le quittancement a bien pu être effectué ou non.
	 */
	@XmlType(name = "CodeQuittancement")
	@XmlEnum(String.class)
	public enum Code {
		/**
		 * La déclaration a bien été quittancée.
		 */
		OK,
		/**
		 * La déclaration n'a pas pu être quittancée. Voir le message d'exception pour plus de détails.
		 */
		EXCEPTION
	}

	/**
	 * La clé qui permet d'identifie la déclaration concernée
	 */
	@XmlElement(required = true)
	public DeclarationImpotOrdinaireKey key;

	/**
	 * Le code de retour du quittancement.
	 */
	@XmlElement(required = true)
	public Code code;

	/**
	 * * Le message de l'exception levée si code = EXCEPTION
	 */
	@XmlElement(required = false)
	public String exceptionMessage;

	/**
	 * Le type de l'exception levée si code = EXCEPTION
	 */
	@XmlElement(required = false)
	public WebServiceExceptionType exceptionType;

	public ReponseQuittancementDeclaration() {
	}

	public ReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, Code code) {
		this.key = key;
		this.code = code;
	}

	public ReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, WebServiceException e) {
		this.key = key;
		this.code = Code.EXCEPTION;
		this.exceptionType = e.getType();
		this.exceptionMessage = e.getMessage();
	}

	public ReponseQuittancementDeclaration(DeclarationImpotOrdinaireKey key, RuntimeException e) {
		this.key = key;
		this.code = Code.EXCEPTION;
		this.exceptionType = WebServiceExceptionType.TECHNICAL;
		this.exceptionMessage = e.getMessage();
	}
}
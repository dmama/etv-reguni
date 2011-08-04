package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Le code de retour du quittancement, qui permet de déterminer si le quittancement a bien pu être effectué ou non.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>taxDeclarationReturnCodeType</i> (xml) / <i>TaxDeclarationReturnCode</i> (client java)
 */
@XmlType(name = "CodeQuittancement")
@XmlEnum(String.class)
public enum CodeQuittancement {
	/**
	 * La déclaration a bien été quittancée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>OK</i>.
	 */
	OK,
	/**
	 * Le contribuable spécifié est inconnu.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ERROR_UNKNOWN_TAXPAYER</i>.
	 */
	ERREUR_CTB_INCONNU,
	/**
	 * Une erreur en relation avec l'assujettissement du contribuable a été détectée (pas de for fiscal principal, ...).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ERROR_TAX_LIABILITY</i>.
	 */
	ERREUR_ASSUJETTISSEMENT_CTB,
	/**
	 * Le contribuable est un débiteur inactif.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ERROR_INACTIVE_DEBTOR</i>.
	 */
	ERREUR_CTB_DEBITEUR_INACTIF,
	/**
	 * La déclaration spécifiée n'existe pas.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ERROR_UNKNOWN_TAX_DECLARATION</i>.
	 */
	ERREUR_DECLARATION_INEXISTANTE,
	/**
	 * La déclaration d'impôt est annnulée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ERROR_CANCELLED_TAX_DECLARATION</i>.
	 */
	ERREUR_DECLARATION_ANNULEE,
	/**
	 * La date de retour est invalide.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ERROR_INVALID_RETURN_DATE</i>.
	 */
	ERREUR_DATE_RETOUR_INVALIDE,
	/**
	 * La déclaration n'a pas pu être quittancée pour une raison inconnue. Voir le message d'exception pour plus de détails.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>EXCEPTION</i>.
	 */
	EXCEPTION
}

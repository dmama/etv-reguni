package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Le code de retour du quittancement, qui permet de déterminer si le quittancement a bien pu être effectué ou non.
 */
@XmlType(name = "CodeQuittancement")
@XmlEnum(String.class)
public enum CodeQuittancement {
	/**
	 * La déclaration a bien été quittancée.
	 */
	OK,
	/**
	 * Le contribuable spécifié est inconnu.
	 */
	ERREUR_CTB_INCONNU,
	/**
	 * Une erreur en relation avec l'assujettissement du contribuable a été détectée (pas de for fiscal principal, ...).
	 */
	ERREUR_ASSUJETTISSEMENT_CTB,
	/**
	 * Le contribuable est un débiteur inactif.
	 */
	ERREUR_CTB_DEBITEUR_INACTIF,
	/**
	 * La déclaration spécifiée n'existe pas.
	 */
	ERREUR_DECLARATION_INEXISTANTE,
	/**
	* La déclaration d'impôt est annnulée.
	 */
	ERREUR_DECLARATION_ANNULEE,
	/**
	 * La date de retour est invalide.
	 */
	ERREUR_DATE_RETOUR_INVALIDE,
	/**
	 * La déclaration n'a pas pu être quittancée pour une raison inconnue. Voir le message d'exception pour plus de détails.
	 */
	EXCEPTION
}

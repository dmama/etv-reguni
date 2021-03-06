package ch.vd.unireg.jms;

import ch.vd.technical.esb.ErrorType;

public enum EsbBusinessCode {

	// note : les codes d'erreur sont définis dans le fichier https://portail.etat-de-vaud.ch/ecm/app6/wcservice/api/node/content/workspace/SpacesStore/8e6ed06a-782f-4ce0-81ae-abfee5d30ffd/TAO_TECH_ESB_Exchange_CodesErreur.xls
	//        en cas d'ajout, il faut modifier le fichier puis créer un JIRA SIFISC sur le composant TAO-Admin pour que les codes soient intégrés TAO-Admin.
	MESSAGE_NON_SUPPORTE(ErrorType.TECHNICAL, "0005", "Ce type de message n'est pas supporté dans ce canal"),
	XML_INVALIDE(ErrorType.TECHNICAL, "0110", "Le message ne correspond pas à la XSD attendue"),
	BAM(ErrorType.TECHNICAL, "0120", "Erreur technique lors de l'envoi d'un message au BAM"),
	IDENTIFICATION_DONNEES_INVALIDES(ErrorType.BUSINESS, "0160", "Données invalides dans la demande d'identification de contribuable"),
	REPONSE_IMPOSSIBLE(ErrorType.BUSINESS, "1051", "Impossible de répondre au message entrant"),
	DROITS_INSUFFISANTS(ErrorType.BUSINESS, "1052", "Droits d'accès insuffisants pour accéder à la requête"),
	CTB_DEBITEUR_INACTIF(ErrorType.BUSINESS, "1061", "Le contribuable est un débiteur inactif et n'est plus accessible en modification"),
	CTB_INEXISTANT(ErrorType.BUSINESS, "1070", "Le contribuable n'existe pas"),
	DPI_INEXISTANT(ErrorType.BUSINESS, "1071", "Le débiteur IS n'existe pas"),
	DECLARATION_NON_QUITTANCEE(ErrorType.BUSINESS, "1080", "La déclaration n'est pas quittancée"),
	DECLARATION_ABSENTE(ErrorType.BUSINESS, "1093", "La déclaration ciblée n'existe pas"),
	DECLARATION_ANNULEE(ErrorType.BUSINESS, "1094", "La déclaration ciblée est annulée"),
	PLUSIEURS_DECLARATIONS(ErrorType.BUSINESS, "1095", "Le contribuable possède plusieurs déclarations dans la période"),
	MAUVAIS_ETAT_DECLARATION(ErrorType.BUSINESS, "1096", "La déclaration est dans un état qui ne permet pas d'appliquer le changement demandé"),
	DATE_OBTENTION_INVALIDE(ErrorType.BUSINESS, "1097", "La date d'obtention du délai est invalide"),
	DATE_DELAI_INVALIDE(ErrorType.BUSINESS, "1098", "La date du délai est invalide"),
	DELAI_INVALIDE(ErrorType.BUSINESS, "1099", "Le délai est invalide"),
	EVT_CIVIL(ErrorType.BUSINESS, "1350", "Données invalides dans l'événement civil entrant"),
	EVT_ENTREPRISE(ErrorType.BUSINESS, "1351", "Données invalides dans l'événement civil entreprise entrant"),
	IAM_INCOMPLET(ErrorType.BUSINESS, "1550", "Données invalides/manquantes dans l'événement IAM entrant"),
	EVT_EXTERNE(ErrorType.BUSINESS, "8451", "Données invalides/manquantes dans l'événement externe entrant"),
	TIERS_INVALIDE(ErrorType.BUSINESS, "8600", "Les données du tiers sont incohérentes");

	private final ErrorType type;
	private final String code;
	private final String libelle;

	EsbBusinessCode(ErrorType type, String code, String libelle) {
		this.type = type;
		this.code = code;
		this.libelle = libelle;
	}

	public ErrorType getType() {
		return type;
	}

	public String getCode() {
		return code;
	}

	public String getLibelle() {
		return libelle;
	}
}

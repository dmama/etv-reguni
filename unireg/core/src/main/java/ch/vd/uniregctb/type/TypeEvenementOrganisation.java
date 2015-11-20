package ch.vd.uniregctb.type;

import java.util.HashMap;
import java.util.Map;

/**
 * Basée sur la spécification evd0022.v1 RCEnt
 *
 * Les numéros sont ceux de RCEnt tels que spécifiés pour "typeOfNoticeType". Les libellés proviennent de la documentation RCEnt:
 * "Norme d'interface entre registres d'entreprises" du 13 mai 2015.
 *
 * @author Raphaël Marmier, 2015-07-13.
 *
 * @since 6.x
 */
public enum TypeEvenementOrganisation {
		FOSC_NOUVELLE_ENTREPRISE(1, "FOSC: Nouvelle entreprise au registre du commerce"),
		FOSC_NOUVELLE_SUCCURSALE(2, "FOSC: Nouvelle succursale au registre du commerce"),
		FOSC_DISSOLUTION_ENTREPRISE(3, "FOSC: Dissolution de l'entreprise au registre du commerce"),
		FOSC_RADIATION_ENTREPRISE(4, "FOSC: Radiation de l'entreprise au registre du commerce"),
		FOSC_RADIATION_SUCCURSALE(5, "FOSC: Radiation de la succursale au registre du commerce"),
		FOSC_REVOCATION_DISSOLUTION_ENTREPRISE(6, "FOSC: Révocation de la dissolution de l'entreprise au registre du commerce"),
		FOSC_REINSCRIPTION_ENTREPRISE(7, "FOSC: Réinscription de l'entreprise au registre du commerce"),
		FOSC_AUTRE_MUTATION(8, "FOSC: Autre mutation au registre du commerce"),
		IMPORTATION_ENTREPRISE(9, "Importation d'une entreprise"),
		FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE(10, "FOSC: Avis préalable d'ouverture de la faillite"),
		FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS(11, "FOSC: Publication de la faillite et appel aux créanciers"),
		FOSC_SUSPENSION_FAILLITE(12, "FOSC: Suspension de la faillite"),
		FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE(13, "FOSC: Etat de collocation et inventaire dans la faillite"),
		FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE(14, "FOSC: Tableau de distribution et décompte final dans la faillite"),
		FOSC_CLOTURE_DE_LA_FAILLITE(15, "FOSC: Clôture de la faillite"),
		FOSC_REVOCATION_DE_LA_FAILLITE(16, "FOSC: Révocation de la faillite"),
		FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE(17, "FOSC: Vente aux enchères forcée d'immeubles dans la faillite"),
		FOSC_ETAT_DES_CHARGES_DANS_FAILLITE(18, "FOSC: Etat des charges dans la faillite"),
		FOSC_COMMUNICATION_DANS_FAILLITE(19, "FOSC: Communication dans la faillite"),
		FOSC_DEMANDE_SURSIS_CONCORDATAIRE(20, "FOSC: Demande de sursis concordataire"),
		FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE(21, "FOSC: Sursis concordataire provisoire"),
		FOSC_SURSIS_CONCORDATAIRE(22, "FOSC: Sursis concordataire"),
		FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT(23, "FOSC: Appel aux créanciers dans le concordat"),
		FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF(24, "FOSC: Audience de liquidation par abandon d'actif"),
		FOSC_PROLONGATION_SURSIS_CONCORDATAIRE(25, "FOSC: Prolongation du sursis concordataire"),
		FOSC_ANNULATION_SURSIS_CONCORDATAIRE(26, "FOSC: Annulation du sursis concordataire"),
		FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS(27, "FOSC: Convocation à l'assemblée des créanciers"),
		FOSC_HOMOLOGATION_DU_CONCORDAT(28, "FOSC: Homologation du concordat"),
		FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT(29, "FOSC: Refus d'homologation du concordat"),
		FOSC_REVOCATION_DU_CONCORDAT(30, "FOSC: Révocation du concordat"),
		FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF(31, "FOSC: Etat de collocation dans la concordat par abandon d'actif"),
		FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF(32, "FOSC: Tableau de distribution et décompte final dans le concordat par abandon d'actif"),
		FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE(33, "FOSC: Concordat de banque et de caisse d'épargne"),
		FOSC_COMMUNICATION_DANS_LE_CONCORDAT(34, "FOSC: Communication dans le concordat"),
		FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE(35, "FOSC: Vente aux enchères forcée d'immeubles dans la poursuite"),
		FOSC_COMMANDEMENT_DE_PAYER(36, "FOSC: Commandement de payer"),
		FOSC_PROCES_VERBAL_SEQUESTRE(37, "FOSC: Procès-verbal de séquestre"),
		FOSC_PROCES_VERBAL_SAISIE(38, "FOSC: Procès-verbal de saisie"),
		FOSC_COMMUNICATION_DANS_LA_POURSUITE(39, "FOSC: Communication dans la poursuite"),
		FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION(40, "FOSC: Appel aux créanciers en suite de fusion ou de scission"),
		FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION(41, "FOSC: Appel aux créanciers en suite de liquidation"),
		FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL(42, "FOSC: Appel aux créanciers en suite de réduction du capital"),
		FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL(43, "FOSC: Appel aux créanciers en suite de transformation d'une SA en une Sàrl selon l'art. 824 CO"),
		FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER(44, "FOSC: Appel aux créanciers en suite de transfert à l'étranger"),
		IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE(45, "IDE: Nouvelle inscription dans le registre IDE"),
		IDE_MUTATION_DANS_REGISTRE(46, "IDE: Mutation dans le registre IDE"),
		IDE_RADIATION_DANS_REGISTRE(47, "IDE: Radiation dans le registre IDE"),
		IDE_REACTIVATION_DANS_REGISTRE(48, "IDE: Réactivation dans le registre IDE"),
		IDE_ANNULATION_DANS_REGISTRE(49, "IDE: Annulation dans le registre IDE"),
		RCPERS_DECES(50, "RCPers: Décès d'une personne"),
		RCPERS_ANNULATION_DECES(51, "RCPers: Annulation du décès d'une personne"),
		RCPERS_DEPART(52, "RCPers: Départ d'une personne"),
		RCPERS_ANNULATION_DEPART(53, "RCPers: Annulation du départ d'une personne"),
		RCPERS_CORRECTION_DONNEES(54, "RCPers: Correction des données relatives à une personne");

	/**
	 * Code technique du type d'événement.
	 */
	private final int id;

	/**
	 * Description textuelle du type de l'événement
	 */
	private final String description;

	/**
	 * Si ce flag est levé, alors l'événement organisation doit être complètement ignoré
	 */
	private final boolean ignore;

	/**
	 * Map permettant d'accéder à un type d'événement par son code.
	 */
	private static final Map<Integer, TypeEvenementOrganisation> typesByCode;

	static {
		typesByCode = new HashMap<>();
		for (TypeEvenementOrganisation type : TypeEvenementOrganisation.values()) {
			typesByCode.put(type.getId(), type);
		}
	}

	/**
	 * Un type d'événement est construit avec son code.
	 *
	 * @param i   code identifiant le type d'événement
	 * @param description description du type d'événement
	 */
	TypeEvenementOrganisation(int i, String description) {
		this(i, description, false);
	}

	/**
	 * Un type d'événement est construit avec son code.
	 *
	 * @param i   code identifiant le type d'événement
	 * @param description description du type d'événement
	 * @param ignore vrai si un événement organisation de ce type doit être complètement ignoré
	 */
	TypeEvenementOrganisation(int i, String description, boolean ignore) {
		this.id = i;
		this.description = description;
		this.ignore = ignore;
	}

	/**
	 * Retourne le code technique du type d'événement.
	 * @return code technique du type d'événement
	 */
	public int getId() {
		return id;
	}

	public String getName() {
		return name();
	}

	/**
	 * Retourne le type d'événement correspondant à un code donné.
	 * @param code  le code de l'événement
	 * @return le type d'événement correspondant à un code donné, null si le code n'a pas été trouvé.
	 */
	public static TypeEvenementOrganisation valueOf(int code) {
		return typesByCode.get(code);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	public String getFullDescription() {
		return description + " (" + id + ')';
	}

	public boolean isIgnore() {
		return ignore;
	}
}

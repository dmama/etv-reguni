package ch.vd.uniregctb.type;

import java.util.HashMap;
import java.util.Map;

/**
 * Type d'événement civil.
 *
 * Longueur de colonne : 37
 *
 * @author Ludovic Bertin
 */
public enum TypeEvenementCivil {

	ETAT_COMPLET(0, "Déclaration de l'état complet d'un individu (eCH-99)", true),// TRAITE (ignoré)

	NAISSANCE(1000, "Naissance"),											// TRAITE naissance ou adoption si enfant inconnu du registre civil
	DECES(2000, "Décés"),													// TRAITE
	ANNUL_DECES(2001, "Annulation décès"),									// TRAITE
	MARIAGE(4000, "Mariage"),												// TRAITE
	ANNUL_MARIAGE(4001, "Annulation mariage"),								// TRAITE
	SEPARATION(6000, "Séparation"),											// TRAITE
	ANNUL_SEPARATION(6001, "Annulation séparation"),						// classes crées (exception remontrée dans le handle)
	RECONCILIATION(7000, "Réconciliation"),									// TRAITE
	ANNUL_RECONCILIATION(7001, "Annulation réconciliation"),				// TRAITE
	DIVORCE(8000, "Divorce"),												// TRAITE
	ANNUL_DIVORCE(8001, "Annulation Divorce"),								// classes crées (exception remontrée dans le handle)
	VEUVAGE(10000, "Veuvage"),												// TRAITE
	ANNUL_VEUVAGE(10001, "Annulation veuvage"),								// TRAITE
	NATIONALITE_SUISSE(12000, "Obtention nationalité suisse"),				// TRAITE
	CORREC_DATE_OBTENTION_NATIONALITE_SUISSE(12010, "Correction date d'obtention nationalité suisse"),// classes crées (exception remontrée dans le handle)
	FIN_NATIONALITE_SUISSE(12020, "Fin obtention nationalité suisse"),		// TRAITE
	CORREC_DATE_FIN_NATIONALITE_SUISSE(12030, "Correction date de fin de nationalité suisse"),// classes crées (exception remontrée dans le handle)
	ANNUL_DATE_FIN_NATIONALITE_SUISSE(12040, "Annulation de fin de nationalité suisse"),// TRAITE
	SUP_NATIONALITE_SUISSE(15000, "Suppression obtention de nationalité suisse"),// TRAITE
	CHGT_CATEGORIE_ETRANGER(16000, "Changement catégorie étranger"),		// TRAITE (=obtention permis)
	FIN_CHANGEMENT_CATEGORIE_ETRANGER(16010, "Fin changement catégorie étranger"),// TRAITE
	ANNUL_CATEGORIE_ETRANGER(16001, "Annulation catégorie étranger"),		// TRAITE
	NATIONALITE_NON_SUISSE(17000, "Obtention nationalité autre que suisse"),// TRAITE
	CORREC_DATE_OBTENTION_NATIONALITE_NON_SUISSE(17010, "Correction date d'obtention nationalité autre que suisse"),// TRAITE
	FIN_NATIONALITE_NON_SUISSE(17020, "Fin obtention nationalité autre que suisse"),// TRAITE
	CORREC_DATE_FIN_NATIONALITE_NON_SUISSE(17030, "Correction date de fin de nationalité autre que suisse"),// TRAITE
	ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE(17040, "Annulation de fin nationalité autre que suisse"),// TRAITE
	SUP_NATIONALITE_NON_SUISSE(17001, "Suppression obtention nationalité autre que suisse"),// TRAITE
	ARRIVEE_DANS_COMMUNE(18000, "Arrivée dans la commune"),					// TRAITE
	SUP_ARRIVEE_DANS_COMMUNE(18001, "Suppression arrivée dans la commune"),	// classes crées (exception remontrée dans le handle)
	ARRIVEE_PRINCIPALE_HS(18020, "Arrivée principale hors suisse"),			// TRAITE
	ARRIVEE_PRINCIPALE_HC(18030, "Arrivée principale hors canton"),			// TRAITE
	ARRIVEE_PRINCIPALE_VAUDOISE(18040, "Arrivée principale vaudoise"),		// TRAITE
	ARRIVEE_SECONDAIRE(18050, "Arrivée secondaire"),						// TRAITE
	ANNUL_ARRIVEE_SECONDAIRE(18051, "Annulation arrivée secondaire"),		// classes crées (exception remontrée dans le handle)
	SUP_INDIVIDU(18011, "Suppression individu"),							// classes crées (exception remontrée dans le handle)
	DEPART_COMMUNE(19000, "Départ de la commune"),							// TRAITE
	SUP_DEPART_COMMUNE(19001, "Suppression départ de la commune"),			// classes crées (exception remontrée dans le handle)
	DEPART_SECONDAIRE(19010, "Départ secondaire"),							// TRAITE
	SUP_DEPART_SECONDAIRE(19011, "Suppression départ secondaire"),			// classes crées (exception remontrée dans le handle)
	DEMENAGEMENT_DANS_COMMUNE(20000, "Déménagement dans la commune"),		// TRAITE uniquement pour individu sans conjoint
	MODIF_ADRESSE_NOTIFICATION(21000, "Modification adresse de notification"),// TRAITE
	MESURE_TUTELLE(25000, "Mesure de tutelle"), 							// TRAITE (par contre bug sur le manytomany avec hibernate sur RapportEntreTiers
	ANNUL_MESURE_TUTELLE(25001, "Annulation mesure de tutelle"),			// TRAITE
	LEVEE_TUTELLE(26000, "Levée de tutelle"),								// TRAITE
	ANNUL_LEVEE_TUTELLE(26001, "Annulation levée de tutelle"),				// TRAITE
	CHGT_CORREC_NOM_PRENOM(29000, "Changement correction nom prénom"),		// TRAITE
	CHGT_SEXE(32000, "Changement de sexe"),									// TRAITE
	CORREC_DATE_NAISSANCE(41010, "Correction date de naissance"),			// TRAITE
	CORREC_DATE_ETAT_CIVIL(41020, "Correction date d'état civil"),			// classes crées (exception remontrée dans le handle)
	CORREC_CONJOINT(41040, "Correction conjoint"),							// classes crées (exception remontrée dans le handle)
	CORREC_PERMIS(41050, "Correction permis"),								// classes crées (exception remontrée dans le handle)
	CORREC_DEBUT_VALIDITE_PERMIS(41060, "Correction début valité permis"),	// classes crées (exception remontrée dans le handle)
	CORREC_ORIGINE(41070, "Correction origine"),							// TRAITE
	CORREC_FIN_VALIDITE_PERMIS(41080, "Correction fin validité permis"),	// classes crées (exception remontrée dans le handle)
	CORREC_DATE_ARRIVEE(42010, "Correction date arrivée"),					// TRAITE
	CORREC_DATE_DEPART(42020, "Correction date départ"),					// classes crées (exception remontrée dans le handle)
	CORREC_ADRESSE(43000, "Correction adresse"),							// même que MODIFICATION_ADRESSE_DE_NOTIFICATION
	CORREC_FILIATION(44000, "Correction filiation"), // TRAITE reconnaissance, désaveu ou adoption si enfant connu du registre civil
	EVENEMENT_TESTING(499999, "Evénement de test"),   // Utile pour tester différents cas d'exception
	CHGT_CORREC_IDENTIFICATION(48000, "Changement correction NAVS13 ou d'un autre identificateur "),		// TRAITE
	;


	/**
	 * Code technique du type d'événement.
	 */
	private final int id;

	/**
	 * Description textuelle du type de l'événement
	 */
	private final String description;

	/**
	 * Si ce flag est levé, alors l'événement civil doit être complètement ignoré
	 */
	private boolean ignore;

	/**
	 * Map permettant d'accéder à un type d'événement par son code.
	 */
	private static Map<Integer, TypeEvenementCivil> typesByCode = null;

	static {
		typesByCode = new HashMap<Integer, TypeEvenementCivil>();
		for (TypeEvenementCivil type : TypeEvenementCivil.values()) {
			typesByCode.put(type.getId(), type);
		}
	}

	/**
	 * Un type d'événement est construit avec son code.
	 *
	 * @param i   code identifiant le type d'événement
	 * @param description description du type d'événement
	 */
	private TypeEvenementCivil(int i, String description) {
		this(i, description, false);
	}

	/**
	 * Un type d'événement est construit avec son code.
	 *
	 * @param i   code identifiant le type d'événement
	 * @param description description du type d'événement
	 * @param ignore vrai si un événement civil de ce type doit être complètement ignoré
	 */
	private TypeEvenementCivil(int i, String description, boolean ignore) {
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
	public static TypeEvenementCivil valueOf(int code) {
		return typesByCode.get(code);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	public String getFullDescription() {
		return description + " (" + id +")";
	}

	public boolean isIgnore() {
		return ignore;
	}
}

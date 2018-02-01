package ch.vd.unireg.editique;

public abstract class ConstantesEditique {

	//
	// Données custom fournies et récupérées par Unireg
	//

	/**
	 * L'attribut des métadonnées utilisé par Unireg (et renvoyé tel quel en cas de réponse) pour y loger son identifiant de document
	 */
	public static final String UNIREG_DOCUMENT_ID = "uniregIdDocument";

	/**
	 * Attribut des métadonnées utilisé par Unireg (et renvoyé tel quel en cas de réponse) pour stocker le type de document (au sens "composition")
	 */
	public static final String UNIREG_TYPE_DOCUMENT = "uniregTypeDocument";

	/**
	 * Attribut des métadonnées utilisé par Unireg (et renvoyé tel quel en cas de réponse) pour stocker le format de fichier attendu en retour (PDF, PCL...)
	 */
	public static final String UNIREG_FORMAT_DOCUMENT = "uniregFormatDocument";

	//
	// Données spécifiques à la composition de document
	//

	public static final String DOCUMENT_TYPE = "documentType";
	public static final String PRINT_MODE = "printMode";
	public static final String RETURN_FORMAT = "returnFormat";
	public static final String ARCHIVE_FLAG = "archive";

	//
	// Différents types de populations
	//

	public static final String POPULATION_PP = "PP";
	public static final String POPULATION_IS = "IS";
	public static final String POPULATION_PM = "PM";

	//
	// Données d'archivage
	//

	public static final String APPLICATION_ARCHIVAGE = "FOLDERS";
	public static final String TYPE_DOSSIER_ARCHIVAGE = "003";

	public static final String TYPE_DOSSIER = "typDossier";
	public static final String NOM_DOSSIER = "nomDossier";
	public static final String TYPE_DOCUMENT = "typDocument";
	public static final String CLE_ARCHIVAGE = "idDocument";
	public static final String TYPE_FORMAT = "typFormat";

	//
	// Données en retour d'éditique
	//

	/**
	 * En retour, peut indiquer un message d'erreur (voir également {@link ch.vd.technical.esb.EsbMessage#ERROR_CODE} et {@link ch.vd.technical.esb.EsbMessage#ERROR_TYPE}
	 */
	public static final String ERROR_MESSAGE = "errorMessage";

}

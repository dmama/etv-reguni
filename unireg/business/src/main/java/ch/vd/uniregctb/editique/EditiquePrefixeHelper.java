package ch.vd.uniregctb.editique;

/**
 * Générateur de préfixes pour les documents Editique
 */
public abstract class EditiquePrefixeHelper {

	//
	// Différents types de préfixes à générer
	//

	private static final String DOCUM = "DOCUM";
	private static final String FOLDE = "FOLDE";
	private static final String HAUT1 = "HAUT1";
	private static final String PERIO = "PERIO";
	private static final String TITIM = "TITIM";
	private static final String IMPCC = "IMPCC";
	private static final String BVRST = "BVRST";

	public static String buildPrefixeInfoDocument(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, DOCUM);
	}

	public static String buildPrefixeInfoArchivage(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, FOLDE);
	}

	public static String buildPrefixeEnteteDocument(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, HAUT1);
	}

	public static String buildPrefixePeriode(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, PERIO);
	}

	public static String buildPrefixeTitreEntete(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, TITIM);
	}

	public static String buildPrefixeImpCcnEntete(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, IMPCC);
	}

	public static String buildPrefixeBvrStandard(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, BVRST);
	}

	private static String buildSpecificPrefix(TypeDocumentEditique typeDocument, String specificSuffix) {
		return String.format("%s%s", typeDocument.getCodeDocumentEditique(), specificSuffix);
	}

}

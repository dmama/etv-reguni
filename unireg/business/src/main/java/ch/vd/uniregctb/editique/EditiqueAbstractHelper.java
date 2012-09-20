package ch.vd.uniregctb.editique;

public abstract class EditiqueAbstractHelper {

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

	//
	// Différentes constantes
	//
	protected static final String ORIGINAL = "ORG";
	protected static final String LOGO_CANTON = "CANT";
	protected static final String POPULATION_PP = "PP";
	protected static final String POPULATION_IS = "IS";

	protected static String buildPrefixeInfoDocument(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, DOCUM);
	}

	protected static String buildPrefixeInfoArchivage(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, FOLDE);
	}

	protected static String buildPrefixeEnteteDocument(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, HAUT1);
	}

	protected static String buildPrefixePeriode(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, PERIO);
	}

	protected static String buildPrefixeTitreEntete(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, TITIM);
	}

	protected static String buildPrefixeImpCcnEntete(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, IMPCC);
	}

	protected static String buildPrefixeBvrStandard(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, BVRST);
	}

	private static String buildSpecificPrefix(TypeDocumentEditique typeDocument, String specificSuffix) {
		return String.format("%s%s", typeDocument.getCodeDocumentEditique(), specificSuffix);
	}
}

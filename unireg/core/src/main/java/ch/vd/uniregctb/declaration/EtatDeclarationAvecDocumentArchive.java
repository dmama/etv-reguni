package ch.vd.uniregctb.declaration;

/**
 * Interface implémentée par les états de déclarations qui ont un document archivé
 * (typiquement les sommations et rappels)
 */
public interface EtatDeclarationAvecDocumentArchive {

	/**
	 * @return la clé de visualisation DPerm du document archivé
	 */
	String getCleDocument();

	/**
	 * @param cle la clé de visualisation DPerm du document archivé
	 */
	void setCleDocument(String cle);
}

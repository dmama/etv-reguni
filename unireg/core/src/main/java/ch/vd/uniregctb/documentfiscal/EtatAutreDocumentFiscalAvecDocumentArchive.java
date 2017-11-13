package ch.vd.uniregctb.documentfiscal;

/**
 * Interface implémentée par les états des autres documents fiscaux qui ont un document archivé.
 */
public interface EtatAutreDocumentFiscalAvecDocumentArchive {

	/**
	 * @return la clé d'archivage du document dans FOLDERS
	 */
	String getCleArchivage();

	/**
	 * @param cleArchivage clé d'archivage du document dans FOLDERS
	 */
	void setCleArchivage(String cleArchivage);

	/**
	 * @return la clé de visualisation DPerm du document archivé
	 */
	String getCleDocument();

	/**
	 * @param cle la clé de visualisation DPerm du document archivé
	 */
	void setCleDocument(String cle);
}

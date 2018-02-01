package ch.vd.unireg.documentfiscal;

import org.jetbrains.annotations.Nullable;

/**
 * Interface implémentée par les états des autres documents fiscaux qui ont un document archivé.
 */
public interface EtatDocumentFiscalAvecDocumentArchive {

	/**
	 * @return la clé d'archivage du document dans FOLDERS
	 */
	String getCleArchivage();

	/**
	 * @param cleArchivage clé d'archivage du document dans FOLDERS
	 */
	void setCleArchivage(String cleArchivage);

	/**
	 * @return la clé de visualisation DPerm du document archivé, ou null
	 */
	@Nullable
	String getCleDocument();

	/**
	 * @param cle la clé de visualisation DPerm du document archivé
	 */
	void setCleDocument(String cle);
}

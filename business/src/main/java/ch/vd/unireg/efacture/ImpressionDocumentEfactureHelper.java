package ch.vd.unireg.efacture;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.type.TypeDocument;

public interface ImpressionDocumentEfactureHelper {

	/**
	 * @param typeDoc
	 * @return
	 */
	TypeDocumentEditique getTypeDocumentEditique(TypeDocument typeDoc);

	/**
	 * Construit le fichier d'impression
	 * @param tiers
	 * @return
	 */
	FichierImpressionDocument remplitDocumentEfacture(ImpressionDocumentEfactureParams params) throws EditiqueException;

	/** Construit les informations d'archivage du document
	 *
	 * @param params
	 * @return
	 */
	String construitIdArchivageDocument(ImpressionDocumentEfactureParams params);

	/**
	 * Construit l'Id du document
	 * @param params
	 * @return
	 */
	String construitIdDocument(ImpressionDocumentEfactureParams params);
}

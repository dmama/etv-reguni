package ch.vd.uniregctb.efacture;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.type.TypeDocument;

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

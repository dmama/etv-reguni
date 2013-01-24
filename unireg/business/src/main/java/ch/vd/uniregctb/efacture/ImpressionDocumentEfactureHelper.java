package ch.vd.uniregctb.efacture;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public interface ImpressionDocumentEfactureHelper {


	/**
	 * Construit le champ idDocument
	 *
	 * @param annee
	 * @param numeroDoc
	 * @param tiers
	 * @return
	 */
	public String construitIdDocument(Integer annee, Integer numeroDoc, Tiers tiers);


	/**
	 * @param typeDoc
	 * @return
	 */
	public TypeDocumentEditique getTypeDocumentEditique(TypeDocument typeDoc);

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

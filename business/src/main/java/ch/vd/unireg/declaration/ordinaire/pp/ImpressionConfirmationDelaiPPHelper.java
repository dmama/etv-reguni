package ch.vd.unireg.declaration.ordinaire.pp;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionConfirmationDelaiPPHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params, String idArchivage) throws EditiqueException;

	String construitIdDocument(DelaiDeclaration delai);

	/**
	 * Construit le champ idDocument pour l'archivage
	 */
	String construitIdArchivageDocument(ImpressionConfirmationDelaiHelperParams params);
}

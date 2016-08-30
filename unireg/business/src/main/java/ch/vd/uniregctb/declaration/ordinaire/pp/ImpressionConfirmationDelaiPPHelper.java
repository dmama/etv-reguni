package ch.vd.uniregctb.declaration.ordinaire.pp;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionConfirmationDelaiPPHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params, String idArchivage) throws EditiqueException;

	String construitIdDocument(DelaiDeclaration delai);

	/**
	 * Construit le champ idDocument pour l'archivage
	 */
	String construitIdArchivageDocument(ImpressionConfirmationDelaiHelperParams params);
}

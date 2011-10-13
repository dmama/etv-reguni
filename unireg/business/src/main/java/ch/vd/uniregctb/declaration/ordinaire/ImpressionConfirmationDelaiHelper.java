package ch.vd.uniregctb.declaration.ordinaire;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionConfirmationDelaiHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException;

	String construitIdDocument(DelaiDeclaration delai);

	/**
	 * Construit le champ idDocument pour l'archivage
	 *
	 * @param params
	 * @return
	 */
	public String construitIdArchivageDocument(ImpressionConfirmationDelaiHelperParams params);
}

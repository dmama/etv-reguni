package ch.vd.uniregctb.declaration.ordinaire;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;


public interface ImpressionSommationDIHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException;

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	String construitIdDocument(DeclarationImpotOrdinaire declaration) ;


	/**
	 * Construit le champ idDocument pour l'archivage
	 *
	 * @param declaration
	 * @return
	 */
	String construitIdArchivageDocument(DeclarationImpotOrdinaire declaration) ;

	/**
	 * Construit le champ idDocument pour l'archivage (avant octobre 2009)
	 *
	 * @param declaration
	 * @return
	 */
	String construitAncienIdArchivageDocument(DeclarationImpotOrdinaire declaration) ;

	/**
	 * Construit le champ idDocument pour l'archivage pour les on-line(avant octobre 2009)
	 *
	 * @param declaration
	 * @return
	 */
	String construitAncienIdArchivageDocumentPourOnLine(DeclarationImpotOrdinaire declaration) ;

}


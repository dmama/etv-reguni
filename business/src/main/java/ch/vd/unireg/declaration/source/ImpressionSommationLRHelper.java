package ch.vd.unireg.declaration.source;

import noNamespace.FichierImpressionDocument;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionSommationLRHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	/**
	 * Génère l'objet pour l'impression de la sommation LR
	 * @param lr
	 * @param dateTraitement
	 * @return
	 * @throws EditiqueException
	 */
	FichierImpressionDocument remplitSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws EditiqueException;

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	String construitIdDocument(DeclarationImpotSource lr) ;


	/**
	 * Construit le champ idDocument pour l'archivage
	 *
	 * @param declaration
	 * @return
	 */
	String construitIdArchivageDocument(DeclarationImpotSource lr) ;

}

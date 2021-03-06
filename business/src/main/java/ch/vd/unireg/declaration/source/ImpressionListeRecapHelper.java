package ch.vd.unireg.declaration.source;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.infra.InfrastructureException;

public interface ImpressionListeRecapHelper {

	/**
	 * Remplit un objet TypFichierImpression
	 *
	 * @param lr
	 * @param traitePar
	 * @return
	 * @throws EditiqueException
	 * @throws InfrastructureException
	 */
	FichierImpressionDocument remplitListeRecap(DeclarationImpotSource lr, String traitePar) throws EditiqueException ;

	/**
	 * Construit le champ idDocument
	 *
	 * @param lr
	 * @return
	 */
	String construitIdDocument(DeclarationImpotSource lr) ;

	/**
	 * Calcul le prefixe
	 *
	 * @param contribuable
	 * @return
	 */

	TypeDocumentEditique getTypeDocumentEditique() ;

}

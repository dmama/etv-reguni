package ch.vd.uniregctb.declaration.source;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionListeRecapHelper {

	/**
	 * Remplit un objet TypFichierImpression
	 *
	 * @param lr
	 * @param traitePar
	 * @return
	 * @throws EditiqueException
	 * @throws ServiceInfrastructureException
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

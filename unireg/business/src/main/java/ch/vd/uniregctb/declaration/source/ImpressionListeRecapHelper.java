package ch.vd.uniregctb.declaration.source;

import noNamespace.FichierImpressionISDocument;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueException;

public interface ImpressionListeRecapHelper {

	/**
	 * Remplit un objet TypFichierImpressionIS
	 *
	 * @param lr
	 * @param traitePar
	 * @return
	 * @throws EditiqueException
	 * @throws InfrastructureException
	 */
	public FichierImpressionISDocument remplitListeRecap(DeclarationImpotSource lr, String traitePar) throws EditiqueException ;

	/**
	 * Construit le champ idDocument
	 *
	 * @param lr
	 * @return
	 */
	public String construitIdDocument(DeclarationImpotSource lr) ;

	/**
	 * Calcul le prefixe
	 *
	 * @param contribuable
	 * @return
	 */

	public String calculPrefixe() ;

}

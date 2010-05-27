package ch.vd.uniregctb.declaration.source;

import noNamespace.TypFichierImpressionIS;
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
	public TypFichierImpressionIS remplitListeRecap(DeclarationImpotSource lr, String traitePar) throws EditiqueException ;

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

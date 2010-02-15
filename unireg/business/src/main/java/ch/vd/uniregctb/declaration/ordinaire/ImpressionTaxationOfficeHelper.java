package ch.vd.uniregctb.declaration.ordinaire;

import noNamespace.TypFichierImpression;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;

public interface ImpressionTaxationOfficeHelper {


	String calculPrefixe();

	TypFichierImpression remplitTaxationOffice(DeclarationImpotOrdinaire declaration) throws EditiqueException;

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) ;

}

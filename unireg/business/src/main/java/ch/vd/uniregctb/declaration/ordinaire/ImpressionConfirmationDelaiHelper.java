package ch.vd.uniregctb.declaration.ordinaire;

import noNamespace.TypFichierImpression;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;

public interface ImpressionConfirmationDelaiHelper {
	
	String calculPrefixe();

	TypFichierImpression remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException;

	public String construitIdDocument(DeclarationImpotOrdinaire declaration);

}

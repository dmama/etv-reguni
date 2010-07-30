package ch.vd.uniregctb.declaration.ordinaire;

import noNamespace.FichierImpressionDocument;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;

public interface ImpressionConfirmationDelaiHelper {
	
	String calculPrefixe();

	FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException;

	public String construitIdDocument(DeclarationImpotOrdinaire declaration);

}

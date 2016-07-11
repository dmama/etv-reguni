package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.util.List;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionDeclarationImpotPersonnesMoralesHelper extends EditiqueAbstractHelper {

	/**
	 * Construction d'un champ IDDocument (qui sert de businessID dans l'envoi ESB)
	 */
	String getIdDocument(DeclarationImpotOrdinairePM declaration);

	/**
	 * @param declaration une déclaration d'impôt PM
	 * @return le type de document tel que vu par Editique
	 */
	TypeDocumentEditique getTypeDocumentEditique(DeclarationImpotOrdinairePM declaration);

	/**
	 * @param declaration une déclaration d'impôt PM
	 * @return un document Editique correspondant à cette déclaration
	 */
	FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException;
}

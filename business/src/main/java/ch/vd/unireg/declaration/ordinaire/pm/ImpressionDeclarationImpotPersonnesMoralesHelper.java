package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.List;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.editique.TypeDocumentEditique;

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

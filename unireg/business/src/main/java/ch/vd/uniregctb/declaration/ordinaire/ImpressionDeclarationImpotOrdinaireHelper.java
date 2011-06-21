package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public interface ImpressionDeclarationImpotOrdinaireHelper {

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) ;

	/**
	 * Alimente un objet Document pour l'impression des DI
	 *
	 *
	 * @param declaration
	 * @param annexes
	 * @param isFromBatch
	 * @return
	 */
	public Document remplitEditiqueSpecifiqueDI(DeclarationImpotOrdinaire declaration, TypFichierImpression typeFichierImpression,
	                                            TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean isFromBatch) throws EditiqueException;

	/**
	 * Calcul le prefixe
	 *
	 * @param declaration
	 * @return
	 */
	public String calculPrefixe(Declaration declaration) ;

}

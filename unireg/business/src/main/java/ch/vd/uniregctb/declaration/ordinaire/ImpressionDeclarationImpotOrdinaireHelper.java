package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;

import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public interface ImpressionDeclarationImpotOrdinaireHelper {

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	public String construitIdDocument(DeclarationImpotOrdinaire declaration);


	/**
	 * Construit le champ idDocument
	 *
	 * @param annee
	 * @param numeroDoc
	 * @param tiers
	 * @return
	 */
	public String construitIdDocument(Integer annee, Integer numeroDoc, Tiers tiers);

	/**
	 * Alimente un objet Document pour l'impression des DI
	 *
	 * @param declaration
	 * @param annexes
	 * @param isFromBatch
	 * @return
	 */
	public Document remplitEditiqueSpecifiqueDI(DeclarationImpotOrdinaire declaration, TypFichierImpression typeFichierImpression,
	                                            TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean isFromBatch) throws EditiqueException;




	public Document remplitEditiqueSpecifiqueDI(InformationsDocumentAdapter informationsDocument, TypFichierImpression typeFichierImpression, List<ModeleFeuilleDocumentEditique> annexes,
	                                            boolean isFromBatch) throws EditiqueException;
	/**
	 * Calcul le prefixe
	 *
	 * @param declaration
	 * @return
	 */
	public TypeDocumentEditique getTypeDocumentEditique(Declaration declaration);

	/**
	 *
	 * @param typeDoc
	 * @return
	 */
	public TypeDocumentEditique getTypeDocumentEditique(TypeDocument typeDoc);


}

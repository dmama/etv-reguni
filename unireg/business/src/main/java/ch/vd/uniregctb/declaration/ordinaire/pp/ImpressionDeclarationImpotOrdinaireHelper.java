package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.List;

import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
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
	String construitIdDocument(DeclarationImpotOrdinairePP declaration);


	/**
	 * Construit le champ idDocument
	 *
	 * @param annee
	 * @param numeroDoc
	 * @param tiers
	 * @return
	 */
	String construitIdDocument(Integer annee, Integer numeroDoc, Tiers tiers);

	/**
	 * Alimente un objet Document pour l'impression des DI
	 *
	 * @param declaration
	 * @param annexes
	 * @return
	 */
	Document remplitEditiqueSpecifiqueDI(DeclarationImpotOrdinairePP declaration, TypFichierImpression typeFichierImpression,
	                                     @Nullable TypeDocument typeDocumentOverride, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException;




	Document remplitEditiqueSpecifiqueDI(InformationsDocumentAdapter informationsDocument, TypFichierImpression typeFichierImpression, List<ModeleFeuilleDocumentEditique> annexes,
	                                     boolean isFromBatchImmeuble) throws EditiqueException;
	/**
	 * Calcul le prefixe
	 *
	 * @param declaration
	 * @return
	 */
	TypeDocumentEditique getTypeDocumentEditique(Declaration declaration);

	/**
	 *
	 * @param typeDoc
	 * @return
	 */
	TypeDocumentEditique getTypeDocumentEditique(TypeDocument typeDoc);


}

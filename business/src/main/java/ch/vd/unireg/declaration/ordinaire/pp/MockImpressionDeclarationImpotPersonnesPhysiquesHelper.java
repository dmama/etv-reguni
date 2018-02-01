package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.List;

import noNamespace.TypFichierImpression;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public class MockImpressionDeclarationImpotPersonnesPhysiquesHelper implements ImpressionDeclarationImpotPersonnesPhysiquesHelper {
	@Override
	public String construitIdDocument(DeclarationImpotOrdinairePP declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdDocument(Integer annee, Integer numeroDoc, Tiers tiers) {
		return null;
	}

	@Override
	public TypFichierImpression.Document remplitEditiqueSpecifiqueDI(DeclarationImpotOrdinairePP declaration, TypFichierImpression typeFichierImpression,
	                                                                 @Nullable TypeDocument typeDocumentOverride, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public TypFichierImpression.Document remplitEditiqueSpecifiqueDI(InformationsDocumentAdapter informationsDocument, TypFichierImpression typeFichierImpression,
	                                                                 List<ModeleFeuilleDocumentEditique> annexes, boolean isFromBatch) throws EditiqueException {
		return null;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(Declaration declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(TypeDocument typeDoc) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

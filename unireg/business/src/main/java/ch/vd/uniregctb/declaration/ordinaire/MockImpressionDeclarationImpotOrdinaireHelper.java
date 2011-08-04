package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import noNamespace.TypFichierImpression;

import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public class MockImpressionDeclarationImpotOrdinaireHelper implements ImpressionDeclarationImpotOrdinaireHelper {
	@Override
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public TypFichierImpression.Document remplitEditiqueSpecifiqueDI(DeclarationImpotOrdinaire declaration, TypFichierImpression typeFichierImpression, TypeDocument typeDocument,
	                                                                 List<ModeleFeuilleDocumentEditique> annexes, boolean isFromBatch) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String calculPrefixe(Declaration declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

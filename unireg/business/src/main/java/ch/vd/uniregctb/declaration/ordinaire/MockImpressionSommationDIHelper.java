package ch.vd.uniregctb.declaration.ordinaire;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;

public class MockImpressionSommationDIHelper implements ImpressionSommationDIHelper {
	@Override
	public String calculPrefixe() {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdArchivageDocument(DeclarationImpotOrdinaire declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitAncienIdArchivageDocument(DeclarationImpotOrdinaire declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitAncienIdArchivageDocumentPourOnLine(DeclarationImpotOrdinaire declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

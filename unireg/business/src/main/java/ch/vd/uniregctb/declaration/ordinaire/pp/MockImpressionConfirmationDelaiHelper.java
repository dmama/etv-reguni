package ch.vd.uniregctb.declaration.ordinaire.pp;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public class MockImpressionConfirmationDelaiHelper implements ImpressionConfirmationDelaiHelper {
	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdDocument(DelaiDeclaration delai) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdArchivageDocument(ImpressionConfirmationDelaiHelperParams params) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

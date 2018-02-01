package ch.vd.unireg.declaration.ordinaire.pp;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public class MockImpressionConfirmationDelaiPPHelper implements ImpressionConfirmationDelaiPPHelper {
	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params, String idArchivage) throws EditiqueException {
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

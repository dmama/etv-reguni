package ch.vd.uniregctb.declaration.source;

import noNamespace.FichierImpressionDocument;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public class MockImpressionSommationLRHelper implements ImpressionSommationLRHelper {

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpressionDocument remplitSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdDocument(DeclarationImpotSource lr) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdArchivageDocument(DeclarationImpotSource lr) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

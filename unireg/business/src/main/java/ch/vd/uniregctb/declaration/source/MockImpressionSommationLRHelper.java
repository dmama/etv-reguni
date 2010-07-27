package ch.vd.uniregctb.declaration.source;

import noNamespace.FichierImpressionISDocument;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueException;

public class MockImpressionSommationLRHelper implements ImpressionSommationLRHelper {

	public String calculPrefixe() {
		throw new IllegalArgumentException("no meant to be called");
	}

	public FichierImpressionISDocument remplitSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	public String construitIdDocument(DeclarationImpotSource lr) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public String construitIdArchivageDocument(DeclarationImpotSource lr) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

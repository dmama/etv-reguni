package ch.vd.uniregctb.declaration.ordinaire.pm;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public class MockImpressionSommationDIPersonnesMoralesHelper implements ImpressionSommationDeclarationImpotPersonnesMoralesHelper {

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration, RegDate dateSommation, boolean batch) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitIdDocument(DeclarationImpotOrdinairePM declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitCleArchivageDocument(DeclarationImpotOrdinairePM declaration) {
		throw new IllegalArgumentException("no meant to be called");
	}
}

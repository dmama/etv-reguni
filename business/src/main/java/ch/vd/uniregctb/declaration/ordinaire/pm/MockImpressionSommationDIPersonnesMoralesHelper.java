package ch.vd.uniregctb.declaration.ordinaire.pm;

import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Contribuable;

public class MockImpressionSommationDIPersonnesMoralesHelper implements ImpressionSommationDeclarationImpotPersonnesMoralesHelper {

	@Nullable
	@Override
	public FichierImpression.Document buildCopieMandataire(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration, RegDate dateSommation, RegDate dateOfficielleEnvoi, boolean batch) throws EditiqueException {
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

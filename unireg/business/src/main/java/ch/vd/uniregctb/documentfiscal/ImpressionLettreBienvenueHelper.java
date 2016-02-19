package ch.vd.uniregctb.documentfiscal;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionLettreBienvenueHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(LettreBienvenue lettre, RegDate dateTraitement, boolean batch) throws EditiqueException;

	String construitIdDocument(LettreBienvenue lettre);

	String construitCleArchivage(LettreBienvenue lettre);
}

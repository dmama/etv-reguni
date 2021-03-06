package ch.vd.unireg.documentfiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionRappelHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(LettreBienvenue lettre, RegDate dateEnvoiRappel, boolean batch) throws EditiqueException;

	String construitIdDocument(LettreBienvenue lettre);

	String construitCleArchivage(LettreBienvenue lettre);
}

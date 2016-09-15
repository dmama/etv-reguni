package ch.vd.uniregctb.documentfiscal;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionLettreLiquidationHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(LettreLiquidation lettre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(LettreLiquidation lettre);

	String construitCleArchivage(LettreLiquidation lettre);

}

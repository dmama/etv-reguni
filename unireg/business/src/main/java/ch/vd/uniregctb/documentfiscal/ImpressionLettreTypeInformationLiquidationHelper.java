package ch.vd.uniregctb.documentfiscal;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionLettreTypeInformationLiquidationHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(LettreTypeInformationLiquidation lettre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(LettreTypeInformationLiquidation lettre);

	String construitCleArchivage(LettreTypeInformationLiquidation lettre);

}

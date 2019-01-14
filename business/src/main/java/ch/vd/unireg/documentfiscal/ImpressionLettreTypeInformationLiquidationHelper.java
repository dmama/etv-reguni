package ch.vd.unireg.documentfiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionLettreTypeInformationLiquidationHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(LettreTypeInformationLiquidation lettre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(LettreTypeInformationLiquidation lettre);

	String construitCleArchivage(LettreTypeInformationLiquidation lettre);

}

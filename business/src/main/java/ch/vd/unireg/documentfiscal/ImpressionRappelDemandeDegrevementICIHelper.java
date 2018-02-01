package ch.vd.unireg.documentfiscal;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.foncier.DemandeDegrevementICI;

public interface ImpressionRappelDemandeDegrevementICIHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(DemandeDegrevementICI demande);

	String construitCleArchivage(DemandeDegrevementICI demande);

}

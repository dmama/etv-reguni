package ch.vd.unireg.documentfiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionAutorisationRadiationRCHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(AutorisationRadiationRC lettre, RegDate dateTraitement, Signataires signataires, ServiceSecuriteService serviceSecurite) throws EditiqueException;

	String construitIdDocument(AutorisationRadiationRC lettre);

	String construitCleArchivage(AutorisationRadiationRC lettre);

}

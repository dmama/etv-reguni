package ch.vd.uniregctb.documentfiscal;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public interface ImpressionAutorisationRadiationRCHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(AutorisationRadiationRC lettre, RegDate dateTraitement, Signataires signataires, ServiceSecuriteService serviceSecurite) throws EditiqueException;

	String construitIdDocument(AutorisationRadiationRC lettre);

	String construitCleArchivage(AutorisationRadiationRC lettre);

}

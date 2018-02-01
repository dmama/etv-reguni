package ch.vd.uniregctb.documentfiscal;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionDemandeBilanFinalHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(DemandeBilanFinal lettre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(DemandeBilanFinal lettre);

	String construitCleArchivage(DemandeBilanFinal lettre);

}

package ch.vd.unireg.fourreNeutre;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionFourreNeutreHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(FourreNeutre fourre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(FourreNeutre fourreNeutre);


}

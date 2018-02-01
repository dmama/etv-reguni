package ch.vd.unireg.fourreNeutre;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionFourreNeutreHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(FourreNeutre fourre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(FourreNeutre fourreNeutre);


}

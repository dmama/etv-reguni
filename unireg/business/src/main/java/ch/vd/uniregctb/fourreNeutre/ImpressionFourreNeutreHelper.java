package ch.vd.uniregctb.fourreNeutre;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionFourreNeutreHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(FourreNeutre fourre, RegDate dateTraitement) throws EditiqueException;

	String construitIdDocument(FourreNeutre fourreNeutre);


}

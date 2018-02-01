package ch.vd.unireg.editique;

import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;

public interface EditiqueAbstractHelper {

	@Nullable
	FichierImpression.Document buildCopieMandataire(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException;

}

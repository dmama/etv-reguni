package ch.vd.unireg.editique;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface EditiqueAbstractHelper {

	@Nullable
	FichierImpression.Document buildCopieMandatairePM(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException;

	@Nullable
	ch.vd.unireg.xml.editique.pp.FichierImpression.Document buildCopieMandatairePP(ch.vd.unireg.xml.editique.pp.FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException;

}

package ch.vd.unireg.documentfiscal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.xml.editique.pm.CTypeImmeuble;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionDemandeDegrevementICIHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpression.Document buildDocument(DemandeDegrevementICI demande, RegDate dateTraitement, boolean duplicata) throws EditiqueException;

	String construitIdDocument(DemandeDegrevementICI demande);

	String construitCleArchivage(DemandeDegrevementICI demande);

	/**
	 * @param demande une demande de dégrèvement ICI
	 * @return les informations "immeuble" à indiquer sur le document
	 * @throws EditiqueException en cas de problème
	 */
	CTypeImmeuble buildInfoImmeuble(DemandeDegrevementICI demande) throws EditiqueException;

	/**
	 * @param entreprise une entreprise
	 * @param dateReference une date
	 * @return une chaîne de caractères désignant le siège de l'entreprise à cette date, <code>null</code> si inconnu
	 */
	@Nullable
	String getSiegeEntreprise(Entreprise entreprise, RegDate dateReference);

	/**
	 * @param demande une demande de dégrèvement ICI
	 * @return le code à barres à imprimer sur le formulaire de demande de dégrèvement
	 */
	String buildCodeBarres(DemandeDegrevementICI demande);

}

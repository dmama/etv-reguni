package ch.vd.uniregctb.documentfiscal;

import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeImmeuble;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.tiers.Entreprise;

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

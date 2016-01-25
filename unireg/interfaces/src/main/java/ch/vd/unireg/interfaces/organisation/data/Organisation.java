package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

public interface Organisation {

	/**
	 * @return l'identifiant de l'organisation dans le système source, utilisé comme clé dans Unireg (id cantonal)
	 */
	long getNumeroOrganisation();

	Capital getCapital(RegDate date);

	/**
	 * @return Liste des sites de l'organisation
	 */
	List<SiteOrganisation> getDonneesSites();

	List<Long> getEnRemplacementDe(RegDate date);

	/**
	 * @return historique des formes juridiques de l'organisation
	 */
	List<DateRanged<FormeLegale>> getFormeLegale();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	/**
	 * @return historique des raisons sociales de l'organisation
	 */
	List<DateRanged<String>> getNom();

	String getNom(RegDate date);

	/**
	 * @return historiques des noms additionnels de l'organisation
	 */
	Map<String, List<DateRanged<String>>> getNomsAdditionnels();

	List<String> getNomsAdditionnels(RegDate date);

	Domicile getSiegePrincipal(RegDate date);

	FormeLegale getFormeLegale(RegDate date);

	List<Capital> getCapitaux();

	/**
	 * Liste des communes de domicile des établissements principaux de l'entreprise, c'est à dire
	 * la liste des communes où l'entreprise à domicilié son siège social.
	 *
	 * @return La liste des sièges de l'entreprise
	 */
	List<Domicile> getSiegesPrincipaux();

	List<Adresse> getAdresses();

	Map<Long, List<DateRanged<Long>>> getEnRemplacementDe();

	List<DateRanged<Long>> getRemplacePar();

	Long getRemplacePar(RegDate date);

	Map<Long, List<DateRanged<Long>>> getTransferDe();

	Map<Long, List<DateRanged<Long>>> getTransfereA();

	List<DateRanged<SiteOrganisation>> getSitePrincipaux();

	DateRanged<SiteOrganisation> getSitePrincipal(RegDate date);

	List<SiteOrganisation> getSitesSecondaires(RegDate date);

	SiteOrganisation getSiteForNo(Long noSite);
}

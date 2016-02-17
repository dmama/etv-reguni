package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

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

	/**
	 * @return historique des formes juridiques de l'organisation
	 */
	List<DateRanged<FormeLegale>> getFormeLegale();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	/**
	 * @return l'dentifiant IDE pour une date donnée, ou null si aucun
	 */
	String getNumeroIDE(RegDate date);

	/**
	 * @return historique des raisons sociales de l'organisation
	 */
	List<DateRanged<String>> getNom();

	/**
	 * @return raison sociale de l'organisation à une date donnée
	 */
	String getNom(RegDate date);

	/**
	 * @return historiques du nom additionnel de l'organisation
	 */
	List<DateRanged<String>> getNomAdditionnel();

	/**
	 * @return nom additionnel de l'organisation à une date donnée
	 */
	String getNomAdditionnel(RegDate date);

	/**
	 * @return siège principal de l'organisation à une date donnée
	 */
	Domicile getSiegePrincipal(RegDate date);

	/**
	 * @return Forme légale de l'organisation à une date donnée
	 */
	FormeLegale getFormeLegale(RegDate date);

	/**
	 * @return historique des capitaux de l'organisation
	 */
	List<Capital> getCapitaux();

	/**
	 * Liste des communes de domicile des établissements principaux de l'entreprise, c'est à dire
	 * la liste des communes où l'entreprise à domicilié son siège social.
	 *
	 * @return La liste des sièges de l'entreprise
	 */
	List<Domicile> getSiegesPrincipaux();

	/**
	 * @return Raison sociale de l'organisation à une date donnée
	 */
	List<Adresse> getAdresses();

	/**
	 * @return historique des sites principaux de l'organisation
	 */
	List<DateRanged<SiteOrganisation>> getSitePrincipaux();

	/**
	 * @return site principal de l'organisation à une date donnée
	 */
	DateRanged<SiteOrganisation> getSitePrincipal(RegDate date);

	/**
	 * @return liste des sites secondaires de l'organisation à une date donnée
	 */
	List<SiteOrganisation> getSitesSecondaires(RegDate date);

	/**
	 * @return le site de l'organisation correspondant à l'identifiant donné
	 */
	SiteOrganisation getSiteForNo(Long noSite);

	/**
	 * @return true si l'organisation est inscrite au RC à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isInscritAuRC(RegDate date);

	/**
	 * @return true si l'organisation est radiée au RC à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieRC(RegDate date);

	/**
	 * @return true si l'organisation est radiée de l'IDE à à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean isRadieIDE(RegDate date);

	/**
	 * @return true si l'organisation possède son siège principal sur Vaud à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 */
	boolean hasSitePrincipalVD(RegDate date);

	/**
	 * @return true si un site de l'organisation est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	boolean hasSiteVD(RegDate date);

}

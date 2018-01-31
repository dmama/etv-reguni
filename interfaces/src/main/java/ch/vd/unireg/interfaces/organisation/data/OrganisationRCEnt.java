package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

/**
 *
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   OrganisationHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.

 */
public class OrganisationRCEnt implements Organisation, Serializable {

	private static final long serialVersionUID = -6557832925666607189L;

	/**
	 * Le numéro technique de l'organisation pour Unireg
	 */
	private final long numeroOrganisation;

	@NotNull
	private final Map<Long, List<DateRanged<Long>>> sites;

	@NotNull
	private final Map<Long, SiteOrganisation> donneesSites;

	public OrganisationRCEnt(long numeroOrganisation,
	                         @NotNull Map<Long, List<DateRanged<Long>>> sites,
	                         @NotNull Map<Long, SiteOrganisation> donneesSites) {
		this.numeroOrganisation = numeroOrganisation;
		this.sites = sites;
		this.donneesSites = donneesSites;
	}

	@Override
	public long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return OrganisationHelper.getNumerosIDEPrincipaux(donneesSites);
	}

	@Override
	public String getNumeroIDE(RegDate date) {
		return OrganisationHelper.valueForDate(getNumeroIDE(), date);
	}

	@Override
	public List<DateRanged<String>> getNumeroRC() {
		return OrganisationHelper.getNumerosRCPrincipaux(donneesSites);
	}

	@Override
	public String getNumeroRC(RegDate date) {
		return OrganisationHelper.valueForDate(getNumeroRC(), date);
	}

	/**
	 * Prepare une liste de plages représantant la succession des sièges des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le siege qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages sièges correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	@Override
	public List<Domicile> getSiegesPrincipaux() {
		return OrganisationHelper.getSiegesPrincipaux(donneesSites);
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return Le siège, ou null si aucun siège valide à la date donnée
	 */
	@Override
	public Domicile getSiegePrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSiegesPrincipaux(), date);
	}

	/**
	 * @return l'historique des formes juridique du site principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return OrganisationHelper.getFormesLegalesPrincipaux(donneesSites);
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return La forme legale, ou null si absente
	 */
	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return OrganisationHelper.valueForDate(getFormeLegale(), date);
	}

	/**
	 * @return l'historique du nom de l'entreprise, c'est-à-dire le nom du site principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<String>> getNom() {
		return OrganisationHelper.getNomsPrincipaux(donneesSites);
	}

	/**
	 * Retourne le nom de l'entreprise à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return le nom
	 */
	@Override
	public String getNom(RegDate date) {
		return OrganisationHelper.valueForDate(getNom(), date);
	}

	/**
	 * @return l'historique du nom additionnel de l'entreprise, c'est-à-dire le nom additionnel du site principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<String>> getNomAdditionnel() {
		return OrganisationHelper.getNomsAdditionnelsPrincipaux(donneesSites);
	}

	/**
	 * Retourne le nom additionnel de l'entreprise à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return le nom
	 */
	@Override
	public String getNomAdditionnel(RegDate date) {
		return OrganisationHelper.valueForDate(getNomAdditionnel(), date);
	}

	/**
	 * Retourne une liste représantant la succession des valeurs de capital de l'entreprise.
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la plage de capital qui lui est contemporaine.
	 *
	 * On recrée l'information du capital dans une nouvelle plage aux limites de la plage type principale qui a permis
	 * de la trouver.
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	@Override
	public List<Capital> getCapitaux() {
		return OrganisationHelper.getCapitaux(donneesSites);
	}

	@Override
	public Capital getCapital(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getCapitaux(), date);
	}

	@Override
	@NotNull
	public List<SiteOrganisation> getDonneesSites() {
		return new ArrayList<>(donneesSites.values());
	}

	@Override
	public List<Adresse> getAdresses() {
		return OrganisationHelper.getAdresses(donneesSites);
	}

	/**
	 * Liste des sites principaux
	 * @return La liste des sites principaux
	 */
	@Override
	public List<DateRanged<SiteOrganisation>> getSitePrincipaux() {
		return OrganisationHelper.getSitePrincipaux(this);
	}

	/**
	 * Le site principal à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date
	 * @return Le site princpial
	 */
	@Override
	public DateRanged<SiteOrganisation> getSitePrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSitePrincipaux(), date);
	}

	/**
	 * Liste des sites secondaire pour une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date pour laquelle on désire la liste des sites secondaires
	 * @return La liste des sites secondaire
	 */
	@Override
	public List<SiteOrganisation> getSitesSecondaires(RegDate date) {
		return OrganisationHelper.getSitesSecondaires(this, date);
	}

	@NotNull
	public Map<Long, List<DateRanged<Long>>> getSites() {
		return sites;
	}

	@Override
	public SiteOrganisation getSiteForNo(Long noSite) {
		return donneesSites.get(noSite);
	}

	/**
	 * Indique si un l'organisation est inscrite au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isInscriteAuRC(RegDate date) {
		return OrganisationHelper.isInscriteAuRC(this, date);
	}

	@Override
	public boolean isConnueInscriteAuRC(RegDate date) {
		return OrganisationHelper.isConnueInscriteAuRC(this, date);
	}

	/**
	 * Indique si un l'organisation est radiée au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieeDuRC(RegDate date) {
		return OrganisationHelper.isRadieeDuRC(this, date);
	}

	/**
	 * Indique si un l'organisation est inscrite à l'IDE à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isInscriteIDE(RegDate date) {
		return OrganisationHelper.isInscriteIDE(this, date);
	}

	/**
	 * Indique si un l'organisation est radiée de l'IDE à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieeIDE(RegDate date) {
		return OrganisationHelper.isRadieeIDE(this, date);
	}

	/**
	 * Indique si un l'organisation possède son siège principal sur Vaud. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean hasSitePrincipalVD(RegDate date) {
		return OrganisationHelper.hasSitePrincipalVD(this, date);
	}

	/**
	 * @return true si un site de l'organisation est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	@Override
	public boolean hasSiteVD(RegDate date) {
		return OrganisationHelper.hasSiteVD(this, date);
	}

	/**
	 * Est-ce que l'organisation a une forme juridique constitutive d'une société individuelle?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'organisation est une société individuelle
	 */
	@Override
	public boolean isSocieteIndividuelle(RegDate date) {
		return OrganisationHelper.isSocieteIndividuelle(this, date);
	}

	/**
	 * Est-ce que l'organisation a une forme juridique constitutive d'une société simple?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'organisation est une société simple
	 */
	@Override
	public boolean isSocieteSimple(RegDate date) {
		return OrganisationHelper.isSocieteSimple(this, date);
	}

	/**
	 * Est-ce que l'organisation a une forme juridique constitutive d'une société de personnes?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'organisation est une société de personnes
	 */
	@Override
	public boolean isSocieteDePersonnes(RegDate date) {
		return OrganisationHelper.isSocieteDePersonnes(this, date);
	}

	/**
	 * Est-ce que l'organisation a une forme juridique d'association ou de fondation?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'organisation est une assocociation ou une fondation
	 */
	@Override
	public boolean isAssociationFondation(RegDate date) {
		return OrganisationHelper.isAssociationFondation(this, date);
	}

	/**
	 * Est-ce que l'organisation a une forme juridique de société à inscription au RC obligatoire?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'organisation est une société à inscription au RC obligatoire
	 */
	@Override
	public boolean isInscriptionRCObligatoire(RegDate date) {
		return OrganisationHelper.isInscriptionRCObligatoire(this, date);
	}

	/**
	 * @return liste des sites de l'organisation domiciliés dans le canton de Vaud (principal ou secondaire), inscrit au RC
	 * et non radiés
	 */
	@Override
	public List<SiteOrganisation> getSuccursalesRCVD(RegDate date) {
		return OrganisationHelper.getSuccursalesRCVD(this, date);
	}
}

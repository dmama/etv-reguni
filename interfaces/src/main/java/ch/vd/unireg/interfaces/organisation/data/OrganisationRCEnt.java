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
	private final Map<Long, List<DateRanged<Long>>> numerosEtablissements;

	@NotNull
	private final Map<Long, EtablissementCivil> donneesEtablissements;

	public OrganisationRCEnt(long numeroOrganisation,
	                         @NotNull Map<Long, List<DateRanged<Long>>> numerosEtablissements,
	                         @NotNull Map<Long, EtablissementCivil> donneesEtablissements) {
		this.numeroOrganisation = numeroOrganisation;
		this.numerosEtablissements = numerosEtablissements;
		this.donneesEtablissements = donneesEtablissements;
	}

	@Override
	public long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return OrganisationHelper.getNumerosIDEPrincipaux(donneesEtablissements);
	}

	@Override
	public String getNumeroIDE(RegDate date) {
		return OrganisationHelper.valueForDate(getNumeroIDE(), date);
	}

	@Override
	public List<DateRanged<String>> getNumeroRC() {
		return OrganisationHelper.getNumerosRCPrincipaux(donneesEtablissements);
	}

	@Override
	public String getNumeroRC(RegDate date) {
		return OrganisationHelper.valueForDate(getNumeroRC(), date);
	}

	/**
	 * Prepare une liste de plages représantant la succession des sièges des établissements principaux
	 *
	 * Pour y arriver, pour chaque établissement civil, on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le siege qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages sièges correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	@Override
	public List<Domicile> getSiegesPrincipaux() {
		return OrganisationHelper.getSiegesPrincipaux(donneesEtablissements);
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
	 * @return l'historique des formes juridique de l'établissement civil principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return OrganisationHelper.getFormesLegalesPrincipaux(donneesEtablissements);
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
	 * @return l'historique du nom de l'entreprise, c'est-à-dire le nom de l'établissement civil principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<String>> getNom() {
		return OrganisationHelper.getNomsPrincipaux(donneesEtablissements);
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
	 * @return l'historique du nom additionnel de l'entreprise, c'est-à-dire le nom additionnel de l'établissement civil principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<String>> getNomAdditionnel() {
		return OrganisationHelper.getNomsAdditionnelsPrincipaux(donneesEtablissements);
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
	 * Pour y arriver, pour chaque établissement civil, on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la plage de capital qui lui est contemporaine.
	 *
	 * On recrée l'information du capital dans une nouvelle plage aux limites de la plage type principale qui a permis
	 * de la trouver.
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	@Override
	public List<Capital> getCapitaux() {
		return OrganisationHelper.getCapitaux(donneesEtablissements);
	}

	@Override
	public Capital getCapital(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getCapitaux(), date);
	}

	@Override
	@NotNull
	public List<EtablissementCivil> getEtablissements() {
		return new ArrayList<>(donneesEtablissements.values());
	}

	@Override
	public List<Adresse> getAdresses() {
		return OrganisationHelper.getAdresses(donneesEtablissements);
	}

	/**
	 * Liste des établissements civils principaux
	 * @return La liste des établissements civils principaux
	 */
	@Override
	public List<DateRanged<EtablissementCivil>> getEtablissementsPrincipaux() {
		return OrganisationHelper.getEtablissementsCivilsPrincipaux(this);
	}

	/**
	 * L'établissement civil principal à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date
	 * @return L'établissement civil princpial
	 */
	@Override
	public DateRanged<EtablissementCivil> getEtablissementPrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getEtablissementsPrincipaux(), date);
	}

	/**
	 * Liste des établissements civils secondaire pour une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date pour laquelle on désire la liste des établissements civils secondaires
	 * @return La liste des établissements civils secondaire
	 */
	@Override
	public List<EtablissementCivil> getEtablissementsSecondaires(RegDate date) {
		return OrganisationHelper.getEtablissementsCivilsSecondaires(this, date);
	}

	@NotNull
	public Map<Long, List<DateRanged<Long>>> getNumerosEtablissements() {
		return numerosEtablissements;
	}

	@Override
	public EtablissementCivil getEtablissementForNo(Long noEtablissementCivil) {
		return donneesEtablissements.get(noEtablissementCivil);
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
	public boolean hasEtablissementPrincipalVD(RegDate date) {
		return OrganisationHelper.hasEtablissementPrincipalVD(this, date);
	}

	/**
	 * @return true si un établissement civil de l'organisation est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	@Override
	public boolean hasEtablissementVD(RegDate date) {
		return OrganisationHelper.hasEtablissementVD(this, date);
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
	 * @return liste des établissements civils de l'organisation domiciliés dans le canton de Vaud (principal ou secondaire), inscrit au RC
	 * et non radiés
	 */
	@Override
	public List<EtablissementCivil> getSuccursalesRCVD(RegDate date) {
		return OrganisationHelper.getSuccursalesRCVD(this, date);
	}
}

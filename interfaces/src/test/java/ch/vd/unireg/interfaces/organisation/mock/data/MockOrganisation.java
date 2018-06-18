package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.TypeEtablissementCivil;

/**
 * Représente un object mock pour une organisation. Le mock fait plusieurs choses:
 *
 * - Il rend modifiables les champs de l'entité.
 * - Il implémente éventuellement des mutations spécifiques, nécessaires dans un
 *   contexte de test.
 *
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs. Dans le cas
 *   présent (Mock), les données sont stockées sous formes d'instantanés. C'est pratique pour la
 *   construction de l'objet, mais nécessite que l'on reconstitue les données sous forme de range.
 *
 *   Les méthodes MockOrganisationHelper.getHisto() et MockOrganisationHelper.reconstitueMultiValeur() sont
 *   là pour ça.
 *
 *   OrganisationHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class MockOrganisation implements Organisation {

	private final long idOrganisation;
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final Map<Long, MockEtablissementCivil> etablissements = new HashMap<>();
	private final List<Adresse> adresses = new ArrayList<>();

	public MockOrganisation(long idOrganisation) {
		this.idOrganisation = idOrganisation;
	}

	public void addDonneesEtablissement(MockEtablissementCivil etablissement) {
		etablissements.put(etablissement.getNumeroEtablissement(), etablissement);
	}

	public void addAdresse(MockAdresse adresse) {
		this.adresses.add(adresse);
		Collections.sort(this.adresses, new DateRangeComparator<>());
	}

	@Override
	public long getNumeroOrganisation() {
		return idOrganisation;
	}

	@Override
	public List<EtablissementCivil> getEtablissements() {
		return new ArrayList<>(etablissements.values());
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return OrganisationHelper.getNumerosIDEPrincipaux(etablissements);
	}

	@Override
	public String getNumeroIDE(RegDate date) {
		return OrganisationHelper.valueForDate(getNumeroIDE(), date);
	}

	@Override
	public List<DateRanged<String>> getNumeroRC() {
		return OrganisationHelper.getNumerosRCPrincipaux(etablissements);
	}

	@Override
	public String getNumeroRC(RegDate date) {
		return OrganisationHelper.valueForDate(getNumeroRC(), date);
	}

	@Override
	public Domicile getSiegePrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSiegesPrincipaux(), date);
	}

	@Override
	public List<DateRanged<EtablissementCivil>> getEtablissementsPrincipaux() {
		return OrganisationHelper.getEtablissementsCivilsPrincipaux(this);
	}

	// Implémentation identique à la classe Organisation
	@Override
	public DateRanged<EtablissementCivil> getEtablissementPrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getEtablissementsPrincipaux(), date);
	}

	// Implémentation identique à la classe Organisation
	@Override
	public List<EtablissementCivil> getEtablissementsSecondaires(RegDate date) {
		return OrganisationHelper.getEtablissementsCivilsSecondaires(this, date);
	}

	@Override
	public EtablissementCivil getEtablissementForNo(Long noEtablissementCivil) {
		return etablissements.get(noEtablissementCivil);
	}

	@Override
	public List<Capital> getCapitaux() {
		Map<Long, EtablissementCivil> map = new HashMap<>();
		for (MockEtablissementCivil mock : etablissements.values()) {
			map.put(mock.getNumeroEtablissement(), mock);
		}
		return OrganisationHelper.getCapitaux(map);
	}

	@Override
	public Capital getCapital(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getCapitaux(), date);
	}

	@Override
	public List<Adresse> getAdresses() {
		return adresses;
	}

	@Override
	public List<Domicile> getSiegesPrincipaux() {
		final List<Domicile> sieges = new ArrayList<>();
		for (MockEtablissementCivil etablissement : etablissements.values()) {
			for (DateRanged<TypeEtablissementCivil> typeSite : etablissement.getTypesEtablissement()) {
				if (typeSite.getPayload() == TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL) {
					sieges.addAll(etablissement.getDomiciles());
				}
			}
		}
		sieges.sort(new DateRangeComparator<>());
		return DateRangeHelper.collate(sieges);
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return OrganisationHelper.getFormesLegalesPrincipaux(etablissements);
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
		return OrganisationHelper.getNomsPrincipaux(etablissements);
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
		return OrganisationHelper.getNomsAdditionnelsPrincipaux(etablissements);
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
		return OrganisationHelper.valueForDate(getNom(), date);
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
	 * @return true si un établissement civil de l'organisation est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	@Override
	public List<EtablissementCivil> getSuccursalesRCVD(RegDate date) {
		return OrganisationHelper.getSuccursalesRCVD(this, date);
	}
}

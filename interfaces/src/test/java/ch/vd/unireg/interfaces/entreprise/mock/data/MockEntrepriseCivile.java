package ch.vd.unireg.interfaces.entreprise.mock.data;

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
import ch.vd.unireg.interfaces.entreprise.data.Capital;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;

/**
 * Représente un object mock pour une entreprise. Le mock fait plusieurs choses:
 *
 * - Il rend modifiables les champs de l'entité.
 * - Il implémente éventuellement des mutations spécifiques, nécessaires dans un
 *   contexte de test.
 *
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs. Dans le cas
 *   présent (Mock), les données sont stockées sous formes d'instantanés. C'est pratique pour la
 *   construction de l'objet, mais nécessite que l'on reconstitue les données sous forme de range.
 *
 *   Les méthodes MockEntrepriseHelper.getHisto() et MockEntrepriseHelper.reconstitueMultiValeur() sont
 *   là pour ça.
 *
 *   EntrepriseHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class MockEntrepriseCivile implements EntrepriseCivile {

	private final long noEntrepriseCivile;
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final Map<Long, MockEtablissementCivil> etablissements = new HashMap<>();
	private final List<Adresse> adresses = new ArrayList<>();

	public MockEntrepriseCivile(long noEntrepriseCivile) {
		this.noEntrepriseCivile = noEntrepriseCivile;
	}

	public void addDonneesEtablissement(MockEtablissementCivil etablissement) {
		etablissements.put(etablissement.getNumeroEtablissement(), etablissement);
	}

	public void addAdresse(MockAdresse adresse) {
		this.adresses.add(adresse);
		Collections.sort(this.adresses, new DateRangeComparator<>());
	}

	@Override
	public long getNumeroEntreprise() {
		return noEntrepriseCivile;
	}

	@Override
	public List<EtablissementCivil> getEtablissements() {
		return new ArrayList<>(etablissements.values());
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return EntrepriseHelper.getNumerosIDEPrincipaux(etablissements);
	}

	@Override
	public String getNumeroIDE(RegDate date) {
		return EntrepriseHelper.valueForDate(getNumeroIDE(), date);
	}

	@Override
	public List<DateRanged<String>> getNumeroRC() {
		return EntrepriseHelper.getNumerosRCPrincipaux(etablissements);
	}

	@Override
	public String getNumeroRC(RegDate date) {
		return EntrepriseHelper.valueForDate(getNumeroRC(), date);
	}

	@Override
	public Domicile getSiegePrincipal(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getSiegesPrincipaux(), date);
	}

	@Override
	public List<DateRanged<EtablissementCivil>> getEtablissementsPrincipaux() {
		return EntrepriseHelper.getEtablissementsCivilsPrincipaux(this);
	}

	// Implémentation identique à la classe EntrepriseCivile
	@Override
	public DateRanged<EtablissementCivil> getEtablissementPrincipal(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getEtablissementsPrincipaux(), date);
	}

	// Implémentation identique à la classe EntrepriseCivile
	@Override
	public List<EtablissementCivil> getEtablissementsSecondaires(RegDate date) {
		return EntrepriseHelper.getEtablissementsCivilsSecondaires(this, date);
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
		return EntrepriseHelper.getCapitaux(map);
	}

	@Override
	public Capital getCapital(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getCapitaux(), date);
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
		return EntrepriseHelper.getFormesLegalesPrincipaux(etablissements);
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
		return EntrepriseHelper.valueForDate(getFormeLegale(), date);
	}

	/**
	 * @return l'historique du nom de l'entreprise, c'est-à-dire le nom de l'établissement civil principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<String>> getNom() {
		return EntrepriseHelper.getNomsPrincipaux(etablissements);
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
		return EntrepriseHelper.valueForDate(getNom(), date);
	}

	/**
	 * @return l'historique du nom additionnel de l'entreprise, c'est-à-dire le nom additionnel de l'établissement civil principal de l'entreprise.
	 */
	@Override
	public List<DateRanged<String>> getNomAdditionnel() {
		return EntrepriseHelper.getNomsAdditionnelsPrincipaux(etablissements);
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
		return EntrepriseHelper.valueForDate(getNom(), date);
	}

	/**
	 * Indique si un l'entreprise est inscrite au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isInscriteAuRC(RegDate date) {
		return EntrepriseHelper.isInscriteAuRC(this, date);
	}

	@Override
	public boolean isConnueInscriteAuRC(RegDate date) {
		return EntrepriseHelper.isConnueInscriteAuRC(this, date);
	}

	/**
	 * Indique si un l'entreprise est radiée au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieeDuRC(RegDate date) {
		return EntrepriseHelper.isRadieeDuRC(this, date);
	}

	/**
	 * Indique si un l'entreprise est inscrite à l'IDE à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isInscriteIDE(RegDate date) {
		return EntrepriseHelper.isInscriteIDE(this, date);
	}

	/**
	 * Indique si un l'entreprise est radiée de l'IDE à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieeIDE(RegDate date) {
		return EntrepriseHelper.isRadieeIDE(this, date);
	}

	/**
	 * Indique si un l'entreprise possède son siège principal sur Vaud. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean hasEtablissementPrincipalVD(RegDate date) {
		return EntrepriseHelper.hasEtablissementPrincipalVD(this, date);
	}

	/**
	 * @return true si un établissement civil de l'entreprise est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	@Override
	public boolean hasEtablissementVD(RegDate date) {
		return EntrepriseHelper.hasEtablissementVD(this, date);
	}

	/**
	 * Est-ce que l'entreprise a une forme juridique constitutive d'une société individuelle?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société individuelle
	 */
	@Override
	public boolean isSocieteIndividuelle(RegDate date) {
		return EntrepriseHelper.isSocieteIndividuelle(this, date);
	}

	/**
	 * Est-ce que l'entreprise a une forme juridique constitutive d'une société simple?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société simple
	 */
	@Override
	public boolean isSocieteSimple(RegDate date) {
		return EntrepriseHelper.isSocieteSimple(this, date);
	}

	/**
	 * Est-ce que l'entreprise a une forme juridique constitutive d'une société de personnes?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société de personnes
	 */
	@Override
	public boolean isSocieteDePersonnes(RegDate date) {
		return EntrepriseHelper.isSocieteDePersonnes(this, date);
	}

	/**
	 * Est-ce que l'entreprise a une forme juridique d'association ou de fondation?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une assocociation ou une fondation
	 */
	@Override
	public boolean isAssociationFondation(RegDate date) {
		return EntrepriseHelper.isAssociationFondation(this, date);
	}

	/**
	 * Est-ce que l'entreprise a une forme juridique de société à inscription au RC obligatoire?
	 * @param date la date pour laquelle on veut l'information, ou si <code>null</code>, la date courante
	 * @return <code>true</code> si l'entreprise est une société à inscription au RC obligatoire
	 */
	@Override
	public boolean isInscriptionRCObligatoire(RegDate date) {
		return EntrepriseHelper.isInscriptionRCObligatoire(this, date);
	}

	/**
	 * @return true si un établissement civil de l'entreprise est domicilié dans le canton de Vaud (principal ou secondaire), false sinon
	 */
	@Override
	public List<EtablissementCivil> getSuccursalesRCVD(RegDate date) {
		return EntrepriseHelper.getSuccursalesRCVD(this, date);
	}
}

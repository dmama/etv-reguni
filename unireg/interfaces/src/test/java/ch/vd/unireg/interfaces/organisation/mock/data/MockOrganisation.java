package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

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
	private final NavigableMap<RegDate, String> nom = new TreeMap<>();
	private final NavigableMap<RegDate, Long> remplacePar = new TreeMap<>();
	private final NavigableMap<RegDate, List<String>> nomsAdditionnels = new TreeMap<>();
	private final NavigableMap<RegDate, List<Long>> enRemplacementDe = new TreeMap<>();
	private final NavigableMap<RegDate, FormeLegale> formeLegale = new TreeMap<>();
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final List<MockSiteOrganisation> sites = new ArrayList<>();
	private final List<Adresse> adresses = new ArrayList<>();

	public MockOrganisation(long idOrganisation, RegDate dateDebut, String nom, FormeLegale formeLegale) {
		this.idOrganisation = idOrganisation;
		changeNom(dateDebut, nom);
		changeFormeLegale(dateDebut, formeLegale);
	}

	public void changeNom(RegDate date, String nouveauNom) {
		MockOrganisationHelper.changeRangedData(nom, date, nouveauNom);
	}

	public void addNom(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauNom) {
		MockOrganisationHelper.addRangedData(nom, dateDebut, dateFin, nouveauNom);
	}

	public void changeFormeLegale(RegDate date, FormeLegale nouvelleFormeLegale) {
		MockOrganisationHelper.changeRangedData(formeLegale, date, nouvelleFormeLegale);
	}

	public void addFormeLegale(RegDate dateDebut, @Nullable RegDate dateFin, FormeLegale nouvelleFormeLegale) {
		MockOrganisationHelper.addRangedData(formeLegale, dateDebut, dateFin, nouvelleFormeLegale);
	}

	public void changeNumeroIDE(RegDate date, String nouveauNumeroIDE) {
		MockOrganisationHelper.changeRangedData(ide, date, nouveauNumeroIDE);
	}

	public void addNumeroIDE(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauNumeroIDE) {
		MockOrganisationHelper.addRangedData(ide, dateDebut, dateFin, nouveauNumeroIDE);
	}

	public void addDonneesSite(MockSiteOrganisation site) {
		sites.add(site);
	}

	public void addNomsAdditionnels(RegDate dateDebut, RegDate dateFin, String... nouveauxNomsAdditionnels) {
		MockOrganisationHelper.addRangedData(nomsAdditionnels, dateDebut, dateFin, nouveauxNomsAdditionnels != null ? Arrays.asList(nouveauxNomsAdditionnels) : Collections.<String>emptyList());
	}

	public void addRemplacePar(RegDate dateDebut, @Nullable RegDate dateFin, Long nouveauRemplacePar) {
		MockOrganisationHelper.addRangedData(remplacePar, dateDebut, dateFin, nouveauRemplacePar);
	}

	public void addEnRemplacementDe(RegDate dateDebut, @Nullable RegDate dateFin, List<Long> nouveauEnRemplacementDe) {
		MockOrganisationHelper.addRangedData(enRemplacementDe, dateDebut, dateFin, nouveauEnRemplacementDe);
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
	public List<SiteOrganisation> getDonneesSites() {
		return new ArrayList<SiteOrganisation>(sites);
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getEnRemplacementDe() {
		return MockOrganisationHelper.reconstitueMultiValeur(enRemplacementDe);
	}

	@Override
	public List<Long> getEnRemplacementDe(RegDate date) {
		return OrganisationHelper.valuesForDate(getEnRemplacementDe(), date);
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return MockOrganisationHelper.getHisto(formeLegale);
	}

	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return OrganisationHelper.valueForDate(getFormeLegale(), date);
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return MockOrganisationHelper.getHisto(ide);
	}

	@Override
	public List<DateRanged<String>> getNom() {
		return MockOrganisationHelper.getHisto(nom);
	}

	@Override
	public String getNom(RegDate date) {
		return OrganisationHelper.valueForDate(getNom(), date);
	}

	@Override
	public Map<String, List<DateRanged<String>>> getNomsAdditionnels() {
		return MockOrganisationHelper.reconstitueMultiValeur(this.nomsAdditionnels);
	}

	@Override
	public List<String> getNomsAdditionnels(RegDate date) {
		return OrganisationHelper.valuesForDate(getNomsAdditionnels(), date);
	}

	@Override
	public Siege getSiegePrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSiegesPrincipaux(), date);
	}

	@Override
	public List<DateRanged<Long>> getRemplacePar() {
		return MockOrganisationHelper.getHisto(remplacePar);
	}

	@Override
	public Long getRemplacePar(RegDate date) {
		return OrganisationHelper.valueForDate(getRemplacePar(), date);
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getTransferDe() {
		throw new NotImplementedException();
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getTransfereA() {
		throw new NotImplementedException();
	}

	@Override
	public List<DateRanged<SiteOrganisation>> getSitePrincipaux() {
		return OrganisationHelper.getSitePrincipaux(this);
	}

	// Implémentation identique à la classe Organisation
	@Override
	public DateRanged<SiteOrganisation> getSitePrincipal(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSitePrincipaux(), date);
	}

	// Implémentation identique à la classe Organisation
	@Override
	public List<SiteOrganisation> getSitesSecondaires(RegDate date) {
		return OrganisationHelper.getSitesSecondaires(this, date);
	}

	@Override
	public List<Capital> getCapitaux() {
		Map<Long, SiteOrganisation> sitesMap = new HashMap<>();
		for (MockSiteOrganisation mock : sites) {
			sitesMap.put(mock.getNumeroSite(), mock);
		}
		return OrganisationHelper.getCapitaux(sitesMap);
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
	public List<Siege> getSiegesPrincipaux() {
		final List<Siege> sieges = new ArrayList<>();
		for (MockSiteOrganisation site : sites) {
			for (DateRanged<TypeDeSite> typeSite : site.getTypeDeSite()) {
				if (typeSite.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
					sieges.addAll(site.getSieges());
				}
			}
		}
		Collections.sort(sieges, new DateRangeComparator<>());
		return DateRangeHelper.collate(sieges);
	}
}

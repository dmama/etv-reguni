package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Représente un object mock pour un site d'organisation. Le mock fait plusieurs choses:
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
public class MockSiteOrganisation implements SiteOrganisation {

	private final long numeroSite;
	private final NavigableMap<RegDate, String> nom = new TreeMap<>();
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final NavigableMap<RegDate, Pair<TypeAutoriteFiscale, Integer>> siege = new TreeMap<>();
	private final NavigableMap<RegDate, TypeDeSite> typeDeSite = new TreeMap<>();
	private final DonneesRegistreIDE donneesRegistreIDE;
	private final DonneesRC donneesRC;
	private final NavigableMap<RegDate, Long> remplacePar = new TreeMap<>();
	private final NavigableMap<RegDate, List<Long>> enRemplacementDe = new TreeMap<>();
	private final List<Adresse> adresses = new ArrayList<>();

	public MockSiteOrganisation(long numeroSite, DonneesRegistreIDE donneesRegistreIDE, DonneesRC donneesRC) {
		this.numeroSite = numeroSite;
		this.donneesRegistreIDE = donneesRegistreIDE;
		this.donneesRC = donneesRC;
	}

	public void changeNom(RegDate date, String nouveauNom) {
		MockOrganisationHelper.changeRangedData(nom, date, nouveauNom);
	}

	public void addNom(RegDate dateDebut, RegDate dateFin, String nouveauNom) {
		MockOrganisationHelper.addRangedData(nom, dateDebut, dateFin, nouveauNom);
	}

	public void changeNumeroIDE(RegDate date, String nouveauNumeroIDE) {
		MockOrganisationHelper.changeRangedData(ide, date, nouveauNumeroIDE);
	}

	public void addNumeroIDE(RegDate dateDebut, RegDate dateFin, String nouveauNumeroIDE) {
		MockOrganisationHelper.addRangedData(ide, dateDebut, dateFin, nouveauNumeroIDE);
	}

	public void addRemplacePar(RegDate dateDebut, @Nullable RegDate dateFin, Long nouveauRemplacePar) {
		MockOrganisationHelper.addRangedData(remplacePar, dateDebut, dateFin, nouveauRemplacePar);
	}

	public void addEnRemplacementDe(RegDate dateDebut, @Nullable RegDate dateFin, List<Long> nouveauEnRemplacementDe) {
		MockOrganisationHelper.addRangedData(enRemplacementDe, dateDebut, dateFin, nouveauEnRemplacementDe);
	}

	public void changeSiege(RegDate date, TypeAutoriteFiscale typeAutoriteFiscale, Integer ofs) {
		Pair payload = null;
		if (typeAutoriteFiscale != null && ofs != null) {
			payload = Pair.of(typeAutoriteFiscale, ofs);
		}
		MockOrganisationHelper.changeRangedData(siege, date, payload);
	}

	public void addSiege(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer ofs) {
		MockOrganisationHelper.addRangedData(siege, dateDebut, dateFin, Pair.of(typeAutoriteFiscale, ofs));
	}

	public void addAdresse(MockAdresse adresse) {
		this.adresses.add(adresse);
		Collections.sort(this.adresses, new DateRangeComparator<>());
	}

	public void changeTypeDeSite(RegDate date, TypeDeSite nouveauType) {
		MockOrganisationHelper.changeRangedData(typeDeSite, date, nouveauType);
	}

	public void addTypeDeSite(RegDate dateDebut, RegDate dateFin, TypeDeSite nouveauType) {
		MockOrganisationHelper.addRangedData(typeDeSite, dateDebut, dateFin, nouveauType);
	}

	@Override
	public long getNumeroSite() {
		return numeroSite;
	}

	@Override
	public Map<String, List<DateRanged<FonctionOrganisation>>> getFonction() {
		throw new NotImplementedException();
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return MockOrganisationHelper.getHisto(ide);
	}

	@Override
	public DonneesRegistreIDE getDonneesRegistreIDE() {
		return donneesRegistreIDE;
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
	public DonneesRC getDonneesRC() {
		return donneesRC;
	}

	@Override
	public List<Siege> getSieges() {
		final List<DateRanged<Pair<TypeAutoriteFiscale, Integer>>> brutto = MockOrganisationHelper.getHisto(siege);
		final List<Siege> sieges = new ArrayList<>(brutto.size());
		for (DateRanged<Pair<TypeAutoriteFiscale, Integer>> ranged : brutto) {
			sieges.add(new Siege(ranged.getDateDebut(), ranged.getDateFin(), ranged.getPayload().getLeft(), ranged.getPayload().getRight()));
		}
		return sieges;
	}

	@Override
	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return MockOrganisationHelper.getHisto(typeDeSite);
	}

	// Implémentation identique à la classe SiteOrganisation
	@Override
	public Siege getSiege(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSieges(), date);	}

	@Override
	public RegDate getDateInscriptionRC(RegDate date) {
		return OrganisationHelper.valueForDate(this.getDonneesRC().getDateInscription(), date);
	}

	@Override
	public List<Adresse> getAdresses() {
		return adresses;
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
	public Map<Long, List<DateRanged<Long>>> getEnRemplacementDe() {
		return MockOrganisationHelper.reconstitueMultiValeur(enRemplacementDe);
	}

	@Override
	public List<Long> getEnRemplacementDe(RegDate date) {
		return OrganisationHelper.valuesForDate(getEnRemplacementDe(), date);
	}
}

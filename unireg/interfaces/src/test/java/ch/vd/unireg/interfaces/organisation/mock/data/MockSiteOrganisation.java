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
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;
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
	private final NavigableMap<RegDate, String> nomAdditionnel = new TreeMap<>();
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final NavigableMap<RegDate, Pair<TypeAutoriteFiscale, Integer>> domicile = new TreeMap<>();
	private final NavigableMap<RegDate, TypeDeSite> typeDeSite = new TreeMap<>();
	private final NavigableMap<RegDate, FormeLegale> formeLegale = new TreeMap<>();
	private final NavigableMap<RegDate, List<PublicationBusiness>> publicationsBusiness = new TreeMap<>();
	private final DonneesRegistreIDE donneesRegistreIDE;
	private final DonneesRC donneesRC;
	private final NavigableMap<RegDate, Long> ideRemplacePar = new TreeMap<>();
	private final NavigableMap<RegDate, Long> ideEnRemplacementDe = new TreeMap<>();
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

	public void changeNomAdditionnel(RegDate date, String nouveauNom) {
		MockOrganisationHelper.changeRangedData(nomAdditionnel, date, nouveauNom);
	}

	public void addNomAdditionnel(RegDate dateDebut, RegDate dateFin, String nouveauNom) {
		MockOrganisationHelper.addRangedData(nomAdditionnel, dateDebut, dateFin, nouveauNom);
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

	public void addNumeroIDE(RegDate dateDebut, RegDate dateFin, String nouveauNumeroIDE) {
		MockOrganisationHelper.addRangedData(ide, dateDebut, dateFin, nouveauNumeroIDE);
	}

	public void addPublicationBusiness(RegDate dateDebut, List<PublicationBusiness> nouvelleListePublicationBusiness) {
		publicationsBusiness.put(dateDebut, nouvelleListePublicationBusiness);
	}

	public void addIdeRemplacePar(RegDate dateDebut, @Nullable RegDate dateFin, Long nouveauRemplacePar) {
		MockOrganisationHelper.addRangedData(ideRemplacePar, dateDebut, dateFin, nouveauRemplacePar);
	}

	public void addIdeEnRemplacementDe(RegDate dateDebut, @Nullable RegDate dateFin, Long nouveauEnRemplacementDe) {
		MockOrganisationHelper.addRangedData(ideEnRemplacementDe, dateDebut, dateFin, nouveauEnRemplacementDe);
	}

	public void changeDomicile(RegDate date, TypeAutoriteFiscale typeAutoriteFiscale, Integer ofs) {
		Pair payload = null;
		if (typeAutoriteFiscale != null && ofs != null) {
			payload = Pair.of(typeAutoriteFiscale, ofs);
		}
		MockOrganisationHelper.changeRangedData(domicile, date, payload);
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return MockOrganisationHelper.getHisto(formeLegale);
	}

	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return OrganisationHelper.valueForDate(getFormeLegale(), date);
	}

	public void addSiege(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer ofs) {
		MockOrganisationHelper.addRangedData(domicile, dateDebut, dateFin, Pair.of(typeAutoriteFiscale, ofs));
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
	public List<DateRanged<String>> getNomAdditionnel() {
		return MockOrganisationHelper.getHisto(nomAdditionnel);
	}

	@Override
	public String getNomAdditionnel(RegDate date) {
		return OrganisationHelper.valueForDate(getNom(), date);
	}

	@Override
	public DonneesRC getDonneesRC() {
		return donneesRC;
	}

	@Override
	public List<Domicile> getDomiciles() {
		final List<DateRanged<Pair<TypeAutoriteFiscale, Integer>>> brutto = MockOrganisationHelper.getHisto(domicile);
		final List<Domicile> domiciles = new ArrayList<>(brutto.size());
		for (DateRanged<Pair<TypeAutoriteFiscale, Integer>> ranged : brutto) {
			domiciles.add(new Domicile(ranged.getDateDebut(), ranged.getDateFin(), ranged.getPayload().getLeft(), ranged.getPayload().getRight()));
		}
		return domiciles;
	}

	@Override
	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return MockOrganisationHelper.getHisto(typeDeSite);
	}

	@Override
	public TypeDeSite getTypeDeSite(RegDate date) {
		return OrganisationHelper.valueForDate(getTypeDeSite(), date);
	}

	// Implémentation identique à la classe SiteOrganisation
	@Override
	public Domicile getDomicile(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getDomiciles(), date);	}

	@Override
	public boolean isSuccursale(RegDate date) {
		return OrganisationHelper.isSuccursale(this, date);
	}

	@Override
	public RegDate getDateInscriptionRC(RegDate date) {
		return OrganisationHelper.valueForDate(this.getDonneesRC().getDateInscription(), date);
	}

	@Override
	public RegDate getDateInscriptionRCVd(RegDate date) {
		return OrganisationHelper.valueForDate(this.getDonneesRC().getDateInscriptionVd(), date);
	}

	@Override
	public List<Adresse> getAdresses() {
		return adresses;
	}

	@Override
	public List<DateRanged<Long>> getIdeRemplacePar() {
		return MockOrganisationHelper.getHisto(ideRemplacePar);
	}

	@Override
	public Long getIdeRemplacePar(RegDate date) {
		return OrganisationHelper.valueForDate(getIdeRemplacePar(), date);
	}

	@Override
	public List<DateRanged<Long>> getIdeEnRemplacementDe() {
		return MockOrganisationHelper.getHisto(ideEnRemplacementDe);
	}

	@Override
	public Long getIdeEnRemplacementDe(RegDate date) {
		return OrganisationHelper.valueForDate(getIdeEnRemplacementDe(), date);
	}

	@Override
	public Map<RegDate, List<PublicationBusiness>> getPublications() {
		return publicationsBusiness;
	}

	@Override
	public List<PublicationBusiness> getPublications(RegDate date) {
		return publicationsBusiness.get(date);
	}

	@Override
	public boolean isInscritAuRC(RegDate date) {
		return OrganisationHelper.isInscritAuRC(this, date);
	}

	@Override
	public boolean isRadieDuRC(RegDate date) {
		return OrganisationHelper.isRadieDuRC(this, date);
	}

	@Override
	public boolean isRadieIDE(RegDate date) {
		return OrganisationHelper.isRadieIDE(this, date);
	}
}

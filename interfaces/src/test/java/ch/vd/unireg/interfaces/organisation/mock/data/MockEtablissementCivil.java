package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
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
import ch.vd.unireg.interfaces.organisation.data.EntrepriseActiviteHelper;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;
import ch.vd.unireg.interfaces.organisation.data.TypeEtablissementCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Représente un object mock pour un établissement civil d'entreprise. Le mock fait plusieurs choses:
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
public class MockEtablissementCivil implements EtablissementCivil {

	private final long numeroSite;
	private final NavigableMap<RegDate, String> nom = new TreeMap<>();
	private final NavigableMap<RegDate, String> nomAdditionnel = new TreeMap<>();
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final NavigableMap<RegDate, String> rc = new TreeMap<>();
	private final NavigableMap<RegDate, Pair<TypeAutoriteFiscale, Integer>> domicile = new TreeMap<>();
	private final NavigableMap<RegDate, TypeEtablissementCivil> typeDeSite = new TreeMap<>();
	private final NavigableMap<RegDate, FormeLegale> formeLegale = new TreeMap<>();
	private final List<PublicationBusiness> publicationsBusiness = new ArrayList<>();
	private final MockDonneesRegistreIDE donneesRegistreIDE;
	private final MockDonneesRC donneesRC;
	private final MockDonneesREE donneesREE;
	private final NavigableMap<RegDate, Long> ideRemplacePar = new TreeMap<>();
	private final NavigableMap<RegDate, Long> ideEnRemplacementDe = new TreeMap<>();
	private final List<Adresse> adresses = new ArrayList<>();

	public MockEtablissementCivil(long numeroSite, MockDonneesRegistreIDE donneesRegistreIDE, MockDonneesRC donneesRC, MockDonneesREE donneesREE) {
		this.numeroSite = numeroSite;
		this.donneesRegistreIDE = donneesRegistreIDE;
		this.donneesRC = donneesRC;
		this.donneesREE = donneesREE;
	}

	public void changeNom(RegDate date, String nouveauNom) {
		MockEntrepriseHelper.changeRangedData(nom, date, nouveauNom);
	}

	public void addNom(RegDate dateDebut, RegDate dateFin, String nouveauNom) {
		MockEntrepriseHelper.addRangedData(nom, dateDebut, dateFin, nouveauNom);
	}

	public void changeNomAdditionnel(RegDate date, String nouveauNom) {
		MockEntrepriseHelper.changeRangedData(nomAdditionnel, date, nouveauNom);
	}

	public void addNomAdditionnel(RegDate dateDebut, RegDate dateFin, String nouveauNom) {
		MockEntrepriseHelper.addRangedData(nomAdditionnel, dateDebut, dateFin, nouveauNom);
	}

	public void changeFormeLegale(RegDate date, FormeLegale nouvelleFormeLegale) {
		MockEntrepriseHelper.changeRangedData(formeLegale, date, nouvelleFormeLegale);
	}

	public void addFormeLegale(RegDate dateDebut, @Nullable RegDate dateFin, FormeLegale nouvelleFormeLegale) {
		MockEntrepriseHelper.addRangedData(formeLegale, dateDebut, dateFin, nouvelleFormeLegale);
	}

	public void changeNumeroIDE(RegDate date, String nouveauNumeroIDE) {
		MockEntrepriseHelper.changeRangedData(ide, date, nouveauNumeroIDE);
	}

	public void addNumeroIDE(RegDate dateDebut, RegDate dateFin, String nouveauNumeroIDE) {
		MockEntrepriseHelper.addRangedData(ide, dateDebut, dateFin, nouveauNumeroIDE);
	}

	public void changeNumeroRC(RegDate date, String nouveauNumeroRC) {
		MockEntrepriseHelper.changeRangedData(rc, date, nouveauNumeroRC);
	}

	public void addNumeroRC(RegDate dateDebut, RegDate dateFin, String nouveauNumeroRC) {
		MockEntrepriseHelper.addRangedData(rc, dateDebut, dateFin, nouveauNumeroRC);
	}

	public void addPublicationBusiness(PublicationBusiness nouvellePublicationBusiness) {
		publicationsBusiness.add(nouvellePublicationBusiness);
	}

	public void addIdeRemplacePar(RegDate dateDebut, @Nullable RegDate dateFin, Long nouveauRemplacePar) {
		MockEntrepriseHelper.addRangedData(ideRemplacePar, dateDebut, dateFin, nouveauRemplacePar);
	}

	public void addIdeEnRemplacementDe(RegDate dateDebut, @Nullable RegDate dateFin, Long nouveauEnRemplacementDe) {
		MockEntrepriseHelper.addRangedData(ideEnRemplacementDe, dateDebut, dateFin, nouveauEnRemplacementDe);
	}

	public void changeDomicile(RegDate date, TypeAutoriteFiscale typeAutoriteFiscale, Integer ofs) {
		Pair<TypeAutoriteFiscale, Integer> payload = null;
		if (typeAutoriteFiscale != null && ofs != null) {
			payload = Pair.of(typeAutoriteFiscale, ofs);
		}
		MockEntrepriseHelper.changeRangedData(domicile, date, payload);
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return MockEntrepriseHelper.getHisto(formeLegale);
	}

	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return EntrepriseHelper.valueForDate(getFormeLegale(), date);
	}

	public void addSiege(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer ofs) {
		MockEntrepriseHelper.addRangedData(domicile, dateDebut, dateFin, Pair.of(typeAutoriteFiscale, ofs));
	}

	public void addAdresse(MockAdresse adresse) {
		this.adresses.add(adresse);
		Collections.sort(this.adresses, new DateRangeComparator<>());
	}

	public void changeTypeEtablissement(RegDate date, TypeEtablissementCivil nouveauType) {
		MockEntrepriseHelper.changeRangedData(typeDeSite, date, nouveauType);
	}

	public void addTypeDeSite(RegDate dateDebut, RegDate dateFin, TypeEtablissementCivil nouveauType) {
		MockEntrepriseHelper.addRangedData(typeDeSite, dateDebut, dateFin, nouveauType);
	}

	@Override
	public long getNumeroEtablissement() {
		return numeroSite;
	}

	@Override
	public Map<String, List<DateRanged<FonctionOrganisation>>> getFonction() {
		throw new NotImplementedException();
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return MockEntrepriseHelper.getHisto(ide);
	}

	@Override
	public String getNumeroIDE(RegDate date) {
		return EntrepriseHelper.valueForDate(getNumeroIDE(), date);
	}

	@Override
	public List<DateRanged<String>> getNumeroRC() {
		return MockEntrepriseHelper.getHisto(rc);
	}

	@Override
	public MockDonneesRegistreIDE getDonneesRegistreIDE() {
		return donneesRegistreIDE;
	}

	@Override
	public List<DateRanged<String>> getNom() {
		return MockEntrepriseHelper.getHisto(nom);
	}

	@Override
	public String getNom(RegDate date) {
		return EntrepriseHelper.valueForDate(getNom(), date);
	}

	@Override
	public List<DateRanged<String>> getNomAdditionnel() {
		return MockEntrepriseHelper.getHisto(nomAdditionnel);
	}

	@Override
	public String getNomAdditionnel(RegDate date) {
		return EntrepriseHelper.valueForDate(getNom(), date);
	}

	@Override
	public MockDonneesRC getDonneesRC() {
		return donneesRC;
	}

	@Override
	public MockDonneesREE getDonneesREE() {
		return donneesREE;
	}

	@Override
	public List<Domicile> getDomiciles() {
		final List<DateRanged<Pair<TypeAutoriteFiscale, Integer>>> brutto = MockEntrepriseHelper.getHisto(domicile);
		final List<Domicile> domiciles = new ArrayList<>(brutto.size());
		for (DateRanged<Pair<TypeAutoriteFiscale, Integer>> ranged : brutto) {
			domiciles.add(new Domicile(ranged.getDateDebut(), ranged.getDateFin(), ranged.getPayload().getLeft(), ranged.getPayload().getRight()));
		}
		return domiciles;
	}

	@Override
	public List<Domicile> getDomicilesEnActivite() {
		return EntrepriseHelper.getDomicilesReels(this, getDomiciles());
	}

	@Override
	public List<DateRanged<TypeEtablissementCivil>> getTypesEtablissement() {
		return MockEntrepriseHelper.getHisto(typeDeSite);
	}

	@Override
	public TypeEtablissementCivil getTypeEtablissement(RegDate date) {
		return EntrepriseHelper.valueForDate(getTypesEtablissement(), date);
	}

	// Implémentation identique à la classe EtablissementCivil
	@Override
	public Domicile getDomicile(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getDomiciles(), date);	}

	@Override
	public boolean isSuccursale(RegDate date) {
		return EntrepriseHelper.isSuccursale(this, date);
	}

	@Override
	public RegDate getDateInscriptionRC(RegDate date) {
		return getInscriptonRC(date)
				.map(InscriptionRC::getDateInscriptionCH)
				.orElse(null);
	}

	@Override
	public RegDate getDateInscriptionRCVd(RegDate date) {
		return getInscriptonRC(date)
				.map(InscriptionRC::getDateInscriptionVD)
				.orElse(null);
	}

	@Override
	public RegDate getDateRadiationRC(RegDate date) {
		return getInscriptonRC(date)
				.map(InscriptionRC::getDateRadiationCH)
				.orElse(null);
	}

	@Override
	public RegDate getDateRadiationRCVd(RegDate date) {
		return getInscriptonRC(date)
				.map(InscriptionRC::getDateRadiationVD)
				.orElse(null);
	}

	private Optional<InscriptionRC> getInscriptonRC(RegDate date) {
		return Optional.of(this)
				.map(MockEtablissementCivil::getDonneesRC)
				.map(MockDonneesRC::getInscription)
				.map(i -> EntrepriseHelper.valueForDate(i, date));
	}

	@Override
	public List<Adresse> getAdresses() {
		return adresses;
	}

	@Override
	public List<DateRanged<Long>> getIdeRemplacePar() {
		return MockEntrepriseHelper.getHisto(ideRemplacePar);
	}

	@Override
	public Long getIdeRemplacePar(RegDate date) {
		return EntrepriseHelper.valueForDate(getIdeRemplacePar(), date);
	}

	@Override
	public List<DateRanged<Long>> getIdeEnRemplacementDe() {
		return MockEntrepriseHelper.getHisto(ideEnRemplacementDe);
	}

	@Override
	public Long getIdeEnRemplacementDe(RegDate date) {
		return EntrepriseHelper.valueForDate(getIdeEnRemplacementDe(), date);
	}

	@Override
	public List<PublicationBusiness> getPublications() {
		return publicationsBusiness;
	}

	@Override
	public List<PublicationBusiness> getPublications(RegDate date) {
			return EntrepriseHelper.getPublications(publicationsBusiness, date);
	}

	@Override
	public RegDate connuAuCivilDepuis() {
		return EntrepriseHelper.connuAuCivilDepuis(this);
	}

	@Override
	public boolean isInscritAuRC(RegDate date) {
		return EntrepriseHelper.isInscritAuRC(this, date);
	}

	@Override
	public boolean isConnuInscritAuRC(RegDate date) {
		return EntrepriseHelper.isConnuInscritAuRC(this, date);
	}

	@Override
	public boolean isActif(RegDate date) {
		return EntrepriseActiviteHelper.isActif(this, date);
	}

	@Override
	public boolean isRadieDuRC(RegDate date) {
		return EntrepriseHelper.isRadieDuRC(this, date);
	}

	@Override
	public boolean isRadieIDE(RegDate date) {
		return EntrepriseHelper.isRadieIDE(this, date);
	}
}

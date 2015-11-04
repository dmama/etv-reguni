package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;

/**
 * @author Raphaël Marmier, 2015-11-04
 */
public class MockDonneesRC implements DonneesRC {
	private NavigableMap<RegDate, StatusRC> status = new TreeMap<>();
	private NavigableMap<RegDate, String> nom = new TreeMap<>();
	private NavigableMap<RegDate, StatusInscriptionRC> statusInscription = new TreeMap<>();
	private NavigableMap<RegDate, Capital> capital = new TreeMap<>();
	private NavigableMap<RegDate, AdresseRCEnt> adresseLegale = new TreeMap<>();
	private NavigableMap<RegDate, String> buts = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateStatus = new TreeMap<>();

	public MockDonneesRC() {};

	public MockDonneesRC(NavigableMap<RegDate, StatusRC> status, NavigableMap<RegDate, String> nom, NavigableMap<RegDate, StatusInscriptionRC> statusInscription,
	                     NavigableMap<RegDate, Capital> capital, NavigableMap<RegDate, AdresseRCEnt> adresseLegale, NavigableMap<RegDate, String> buts, NavigableMap<RegDate, RegDate> dateStatus) {
		this.status = status;
		this.nom = nom;
		this.statusInscription = statusInscription;
		this.capital = capital;
		this.adresseLegale = adresseLegale;
		this.buts = buts;
		this.dateStatus = dateStatus;
	}

	@Override
	public List<AdresseRCEnt> getAdresseLegale() {
		return new ArrayList<>(adresseLegale.values());
	}

	public void changeAdresseLegale(RegDate date, AdresseRCEnt nouvelleAdresseLegale) {
		MockOrganisationHelper.changeRangedData(adresseLegale, date, nouvelleAdresseLegale);
	}

	public void addAdresseLegale(RegDate dateDebut, @Nullable RegDate dateFin, AdresseRCEnt nouvelleAdresseLegale) {
		MockOrganisationHelper.addRangedData(adresseLegale, dateDebut, dateFin, nouvelleAdresseLegale);
	}

	@Override
	public List<Capital> getCapital() {
		return new ArrayList<>(capital.values());
	}

	public void changeCapital(RegDate date, Capital nouveauCapital) {
		MockOrganisationHelper.changeRangedData(capital, date, nouveauCapital);
	}

	public void addCapital(RegDate dateDebut, @Nullable RegDate dateFin, Capital nouveauCapital) {
		MockOrganisationHelper.addRangedData(capital, dateDebut, dateFin, nouveauCapital);
	}

	@Override
	public List<DateRanged<String>> getNom() {
		return MockOrganisationHelper.getHisto(nom);
	}

	public void changeNom(RegDate date, String nouveauNom) {
		MockOrganisationHelper.changeRangedData(nom, date, nouveauNom);
	}

	public void addNom(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauNom) {
		MockOrganisationHelper.addRangedData(nom, dateDebut, dateFin, nouveauNom);
	}

	@Override
	public List<DateRanged<StatusRC>> getStatus() {
		return MockOrganisationHelper.getHisto(status);
	}

	public void changeStatus(RegDate date, StatusRC nouveauStatus) {
		MockOrganisationHelper.changeRangedData(status, date, nouveauStatus);
	}

	public void addStatus(RegDate dateDebut, @Nullable RegDate dateFin, StatusRC nouveauStatus) {
		MockOrganisationHelper.addRangedData(status, dateDebut, dateFin, nouveauStatus);
	}

	@Override
	public List<DateRanged<StatusInscriptionRC>> getStatusInscription() {
		return MockOrganisationHelper.getHisto(statusInscription);
	}

	public void changeStatusInscription(RegDate date, StatusInscriptionRC nouveauStatusInscription) {
		MockOrganisationHelper.changeRangedData(statusInscription, date, nouveauStatusInscription);
	}

	public void addStatusInscription(RegDate dateDebut, @Nullable RegDate dateFin, StatusInscriptionRC nouveauStatusInscription) {
		MockOrganisationHelper.addRangedData(statusInscription, dateDebut, dateFin, nouveauStatusInscription);
	}

	@Override
	public List<DateRanged<String>> getButs() {
		return MockOrganisationHelper.getHisto(buts);
	}

	public void changeButs(RegDate date, String nouveauxButs) {
		MockOrganisationHelper.changeRangedData(buts, date, nouveauxButs);
	}

	public void addButs(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauxButs) {
		MockOrganisationHelper.addRangedData(buts, dateDebut, dateFin, nouveauxButs);
	}

	@Override
	public List<DateRanged<RegDate>> getDateStatus() {
		return MockOrganisationHelper.getHisto(dateStatus);
	}

	public void changeDateStatus(RegDate date, RegDate nouvelleDateStatus) {
		MockOrganisationHelper.changeRangedData(dateStatus, date, nouvelleDateStatus);
	}

	public void addDateStatus(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateStatus) {
		MockOrganisationHelper.addRangedData(dateStatus, dateDebut, dateFin, nouvelleDateStatus);
	}
}

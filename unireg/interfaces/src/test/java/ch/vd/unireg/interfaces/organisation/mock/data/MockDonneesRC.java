package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;

/**
 * @author Raphaël Marmier, 2015-11-04
 */
public class MockDonneesRC implements DonneesRC {

	private NavigableMap<RegDate, StatusInscriptionRC> statusInscription = new TreeMap<>();
	private NavigableMap<RegDate, RaisonDeDissolutionRC> raisonDeDissolutionVd = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateInscription = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateInscriptionVd = new TreeMap<>();
	private NavigableMap<RegDate, Capital> capital = new TreeMap<>();
	private NavigableMap<RegDate, AdresseLegaleRCEnt> adresseLegale = new TreeMap<>();
	private NavigableMap<RegDate, String> buts = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateStatus = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateRadiation = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateRadiationVd = new TreeMap<>();
	private List<EntreeJournalRC> entreesJournal = new ArrayList<>();

	public MockDonneesRC() {};

	public MockDonneesRC(NavigableMap<RegDate, StatusInscriptionRC> statusInscription,
	                     NavigableMap<RegDate, Capital> capital, NavigableMap<RegDate, AdresseLegaleRCEnt> adresseLegale, NavigableMap<RegDate, String> buts,
	                     NavigableMap<RegDate, RegDate> dateStatus, NavigableMap<RegDate, RegDate> dateRadiation) {
		this.statusInscription = statusInscription;
		this.capital = capital;
		this.adresseLegale = adresseLegale;
		this.buts = buts;
		this.dateStatus = dateStatus;
		this.dateRadiation = dateRadiation;
	}

	@Override
	public List<AdresseLegaleRCEnt> getAdresseLegale() {
		return new ArrayList<>(adresseLegale.values());
	}

	public void changeAdresseLegale(RegDate date, AdresseLegaleRCEnt nouvelleAdresseLegale) {
		throw new UnsupportedOperationException();
	}

	public void addAdresseLegale(RegDate dateDebut, @Nullable RegDate dateFin, AdresseLegaleRCEnt nouvelleAdresseLegale) {
		final Map.Entry<RegDate, AdresseLegaleRCEnt> previousEntry = adresseLegale.lastEntry();
		if (previousEntry != null) {
			final AdresseLegaleRCEnt previous = previousEntry.getValue();
			adresseLegale.put(previous.getDateDebut(), (new AdresseLegaleRCEnt(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getLocalite(), previous.getNumero(),
			                                                                   previous.getNumeroAppartement(), previous.getNumeroOrdrePostal(), previous.getNumeroPostal(),
			                                                                   previous.getNumeroPostalComplementaire(), previous.getNoOfsPays(), previous.getRue(),
			                                                                   previous.getTitre(), previous.getEgid(), previous.getCasePostale())));
		}
		MockOrganisationHelper.addRangedData(adresseLegale, dateDebut, dateFin, nouvelleAdresseLegale);
	}

	@Override
	public AdresseLegaleRCEnt getAdresseLegale(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getAdresseLegale(), date);
	}

	@Override
	public List<Capital> getCapital() {
		return new ArrayList<>(capital.values());
	}

	public void changeCapital(RegDate date, Capital nouveauCapital) {
		throw new UnsupportedOperationException();
	}

	public void addCapital(RegDate dateDebut, @Nullable RegDate dateFin, Capital nouveauCapital) {
		final Map.Entry<RegDate, Capital> previousEntry = capital.lastEntry();
		if (previousEntry != null) {
			final Capital previous = previousEntry.getValue();
			capital.put(previous.getDateDebut(), new Capital(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getTypeDeCapital(), previous.getDevise(),
			                                                 previous.getCapitalLibere(), previous.getRepartition()));
		}
		MockOrganisationHelper.addRangedData(capital, dateDebut, dateFin, nouveauCapital);
	}

	public List<DateRanged<StatusInscriptionRC>> getStatusREE() {
		return MockOrganisationHelper.getHisto(statusInscription);
	}

	@Override
	public StatusInscriptionRC getStatusInscription(RegDate date) {
		return OrganisationHelper.valueForDate(getStatusREE(), date);
	}

	public void changeStatusInscription(RegDate date, StatusInscriptionRC nouveauStatusInscription) {
		MockOrganisationHelper.changeRangedData(statusInscription, date, nouveauStatusInscription);
	}

	public void addStatusInscription(RegDate dateDebut, @Nullable RegDate dateFin, StatusInscriptionRC nouveauStatusInscription) {
		MockOrganisationHelper.addRangedData(statusInscription, dateDebut, dateFin, nouveauStatusInscription);
	}

	@Override
	public List<DateRanged<RaisonDeDissolutionRC>> getRaisonDeDissolutionVd() {
		return MockOrganisationHelper.getHisto(raisonDeDissolutionVd);
	}

	@Override
	public RaisonDeDissolutionRC getRaisonDeDissolutionVd(RegDate date) {
		return OrganisationHelper.valueForDate(getRaisonDeDissolutionVd(), date);
	}

	public void changeRaisonDeDissolutionVd(RegDate date, RaisonDeDissolutionRC nouvelleRaisonDeDissolution) {
		MockOrganisationHelper.changeRangedData(raisonDeDissolutionVd, date, nouvelleRaisonDeDissolution);
	}

	public void addRaisonDeDissolutionVd(RegDate dateDebut, @Nullable RegDate dateFin, RaisonDeDissolutionRC nouvelleRaisonDeDissolution) {
		MockOrganisationHelper.addRangedData(raisonDeDissolutionVd, dateDebut, dateFin, nouvelleRaisonDeDissolution);
	}

	public List<DateRanged<RegDate>> getDateInscriptionREE() {
		return MockOrganisationHelper.getHisto(dateInscription);
	}

	public void changeDateInscription(RegDate date, RegDate nouvelleDateInscription) {
		MockOrganisationHelper.changeRangedData(dateInscription, date, nouvelleDateInscription);
	}

	public void addDateInscription(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateInscription) {
		MockOrganisationHelper.addRangedData(dateInscription, dateDebut, dateFin, nouvelleDateInscription);
	}

	@Override
	public RegDate getDateInscription(RegDate date) {
		return OrganisationHelper.valueForDate(getDateInscriptionREE(), date);
	}

	@Override
	public List<DateRanged<RegDate>> getDateInscriptionVd() {
		return MockOrganisationHelper.getHisto(dateInscriptionVd);
	}

	public void changeDateInscriptionVd(RegDate date, RegDate nouvelleDateInscriptionVd) {
		MockOrganisationHelper.changeRangedData(dateInscriptionVd, date, nouvelleDateInscriptionVd);
	}

	public void addDateInscriptionVd(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateInscriptionVd) {
		MockOrganisationHelper.addRangedData(dateInscriptionVd, dateDebut, dateFin, nouvelleDateInscriptionVd);
	}

	@Override
	public RegDate getDateInscriptionVd(RegDate date) {
		return OrganisationHelper.valueForDate(getDateInscriptionVd(), date);
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
	public List<DateRanged<RegDate>> getDateStatuts() {
		return MockOrganisationHelper.getHisto(dateStatus);
	}

	public void changeDateStatus(RegDate date, RegDate nouvelleDateStatus) {
		MockOrganisationHelper.changeRangedData(dateStatus, date, nouvelleDateStatus);
	}

	public void addDateStatus(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateStatus) {
		MockOrganisationHelper.addRangedData(dateStatus, dateDebut, dateFin, nouvelleDateStatus);
	}

	@Override
	public List<DateRanged<RegDate>> getDateRadiation() {
		return MockOrganisationHelper.getHisto(dateRadiation);
	}

	@Override
	public RegDate getDateRadiation(RegDate date) {
		return OrganisationHelper.valueForDate(getDateRadiation(), date);
	}

	public void changeDateRadiation(RegDate date, RegDate nouvelleDateRadiation) {
		MockOrganisationHelper.changeRangedData(dateRadiation, date, nouvelleDateRadiation);
	}

	public void addDateRadiation(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateRadiation) {
		MockOrganisationHelper.addRangedData(dateRadiation, dateDebut, dateFin, nouvelleDateRadiation);
	}

	@Override
	public List<DateRanged<RegDate>> getDateRadiationVd() {
		return MockOrganisationHelper.getHisto(dateRadiationVd);
	}

	@Override
	public RegDate getDateRadiationVd(RegDate date) {
		return OrganisationHelper.valueForDate(getDateRadiationVd(), date);
	}

	public void changeDateRadiationVd(RegDate date, RegDate nouvelleDateRadiation) {
		MockOrganisationHelper.changeRangedData(dateRadiationVd, date, nouvelleDateRadiation);
	}

	public void addDateRadiationVd(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateRadiation) {
		MockOrganisationHelper.addRangedData(dateRadiationVd, dateDebut, dateFin, nouvelleDateRadiation);
	}

	@Override
	public List<EntreeJournalRC> getEntreesJournal() {
		return entreesJournal;
	}

	@Override
	public List<EntreeJournalRC> getEntreesJournal(RegDate date) {
		return OrganisationHelper.getEntreesJournal(entreesJournal, date);
	}

	public void addEntreeJournal(EntreeJournalRC entreeJournal) {
		this.entreesJournal.add(entreeJournal);
	}

}

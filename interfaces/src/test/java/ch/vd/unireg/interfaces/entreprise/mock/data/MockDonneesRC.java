package ch.vd.unireg.interfaces.entreprise.mock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.Capital;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRC;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;

/**
 * @author Raphaël Marmier, 2015-11-04
 */
public class MockDonneesRC implements DonneesRC {

	private final NavigableMap<RegDate, InscriptionRC> inscription;
	private final NavigableMap<RegDate, Capital> capital;
	private final NavigableMap<RegDate, AdresseLegaleRCEnt> adresseLegale;
	private final NavigableMap<RegDate, String> buts;
	private final NavigableMap<RegDate, RegDate> dateStatus;
	private final List<EntreeJournalRC> entreesJournal = new ArrayList<>();

	public MockDonneesRC() {
		this(new TreeMap<RegDate, InscriptionRC>(),
		     new TreeMap<RegDate, Capital>(),
		     new TreeMap<RegDate, AdresseLegaleRCEnt>(),
		     new TreeMap<RegDate, String>(),
		     new TreeMap<RegDate, RegDate>());
	};

	public MockDonneesRC(NavigableMap<RegDate, InscriptionRC> inscription,
	                     NavigableMap<RegDate, Capital> capital,
	                     NavigableMap<RegDate, AdresseLegaleRCEnt> adresseLegale,
	                     NavigableMap<RegDate, String> buts,
	                     NavigableMap<RegDate, RegDate> dateStatus) {
		this.inscription = inscription;
		this.capital = capital;
		this.adresseLegale = adresseLegale;
		this.buts = buts;
		this.dateStatus = dateStatus;
	}

	@Override
	public List<AdresseLegaleRCEnt> getAdresseLegale() {
		return new ArrayList<>(adresseLegale.values());
	}

	public void changeAdresseLegale(RegDate date, AdresseLegaleRCEnt nouvelleAdresseLegale) {
		throw new UnsupportedOperationException();
	}

	public void addAdresseLegale(AdresseLegaleRCEnt nouvelleAdresseLegale) {
		final RegDate dateDebut = nouvelleAdresseLegale.getDateDebut();
		final RegDate dateFin = nouvelleAdresseLegale.getDateFin();

		final Map.Entry<RegDate, AdresseLegaleRCEnt> previousEntry = adresseLegale.lastEntry();
		if (previousEntry != null) {
			final AdresseLegaleRCEnt previous = previousEntry.getValue();
			adresseLegale.put(previous.getDateDebut(), (new AdresseLegaleRCEnt(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getLocalite(), previous.getNumero(),
			                                                                   previous.getNumeroAppartement(), previous.getNumeroOrdrePostal(), previous.getNumeroPostal(),
			                                                                   previous.getNumeroPostalComplementaire(), previous.getNoOfsPays(), previous.getRue(),
			                                                                   previous.getTitre(), previous.getEgid(), previous.getCasePostale())));
		}
		MockEntrepriseHelper.addRangedData(adresseLegale, dateDebut, dateFin, nouvelleAdresseLegale);
	}

	@Override
	public AdresseLegaleRCEnt getAdresseLegale(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getAdresseLegale(), date);
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
		MockEntrepriseHelper.addRangedData(capital, dateDebut, dateFin, nouveauCapital);
	}

	@Override
	public InscriptionRC getInscription(RegDate date) {
		return EntrepriseHelper.valueForDate(getInscription(), date);
	}

	@Override
	public List<DateRanged<InscriptionRC>> getInscription() {
		return MockEntrepriseHelper.getHisto(inscription);
	}

	public void changeInscription(RegDate date, InscriptionRC nouvelleInscription) {
		MockEntrepriseHelper.changeRangedData(inscription, date, nouvelleInscription);
	}

	public void addInscription(RegDate dateDebut, @Nullable RegDate dateFin, InscriptionRC inscription) {
		MockEntrepriseHelper.addRangedData(this.inscription, dateDebut, dateFin, inscription);
	}

	public List<DateRanged<String>> getButs() {
		return MockEntrepriseHelper.getHisto(buts);
	}

	public void changeButs(RegDate date, String nouveauxButs) {
		MockEntrepriseHelper.changeRangedData(buts, date, nouveauxButs);
	}

	public void addButs(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauxButs) {
		MockEntrepriseHelper.addRangedData(buts, dateDebut, dateFin, nouveauxButs);
	}

	@Override
	public List<DateRanged<RegDate>> getDateStatuts() {
		return MockEntrepriseHelper.getHisto(dateStatus);
	}

	public void changeDateStatus(RegDate date, RegDate nouvelleDateStatus) {
		MockEntrepriseHelper.changeRangedData(dateStatus, date, nouvelleDateStatus);
	}

	public void addDateStatus(RegDate dateDebut, @Nullable RegDate dateFin, RegDate nouvelleDateStatus) {
		MockEntrepriseHelper.addRangedData(dateStatus, dateDebut, dateFin, nouvelleDateStatus);
	}

	@Override
	public List<EntreeJournalRC> getEntreesJournal() {
		return entreesJournal;
	}

	@Override
	public List<EntreeJournalRC> getEntreesJournalPourDatePublication(RegDate date) {
		return EntrepriseHelper.getEntreesJournalPourDatePublication(entreesJournal, date);
	}

	public void addEntreeJournal(EntreeJournalRC entreeJournal) {
		this.entreesJournal.add(entreeJournal);
	}

}

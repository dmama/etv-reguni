package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesREE;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.StatusREE;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class MockDonneesREE implements DonneesREE {

	private NavigableMap<RegDate, StatusREE> statusREE = new TreeMap<>();
	private NavigableMap<RegDate, RegDate> dateInscriptionREE = new TreeMap<>();

	public MockDonneesREE() {}

	public MockDonneesREE(NavigableMap<RegDate, StatusREE> statusREE, NavigableMap<RegDate, RegDate> dateInscriptionREE) {
		this.statusREE = statusREE;
		this.dateInscriptionREE = dateInscriptionREE;
	}

	@NotNull
	@Override
	public List<DateRanged<StatusREE>> getStatusREE() {
		return MockOrganisationHelper.getHisto(statusREE);
	}

	@Override
	public StatusREE getStatusREE(RegDate date) {
		return OrganisationHelper.valueForDate(getStatusREE(), date);
	}

	public void changeStatus(RegDate date, StatusREE nouveauStatus) {
		MockOrganisationHelper.changeRangedData(statusREE, date, nouveauStatus);
	}

	public void addStatus(RegDate dateDebut, RegDate dateFin, StatusREE nouveauStatus) {
		MockOrganisationHelper.addRangedData(statusREE, dateDebut, dateFin, nouveauStatus);
	}

	@Override
	public List<DateRanged<RegDate>> getDateInscriptionREE() {
		return MockOrganisationHelper.getHisto(dateInscriptionREE);
	}

	@Override
	public RegDate getDateInscriptionREE(RegDate date) {
		return OrganisationHelper.valueForDate(getDateInscriptionREE(), date);
	}

	public void changeDateInscriptionREE(RegDate date, RegDate nouvelleDateInscription) {
		MockOrganisationHelper.changeRangedData(dateInscriptionREE, date, nouvelleDateInscription);
	}

	public void addTypeOrganisation(RegDate dateDebut, RegDate dateFin, RegDate nouvelleDateInscription) {
		MockOrganisationHelper.addRangedData(dateInscriptionREE, dateDebut, dateFin, nouvelleDateInscription);
	}

}

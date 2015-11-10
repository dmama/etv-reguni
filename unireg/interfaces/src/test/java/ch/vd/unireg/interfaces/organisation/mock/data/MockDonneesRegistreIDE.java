package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseRCEnt;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.RaisonLiquidationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class MockDonneesRegistreIDE implements DonneesRegistreIDE {

	private NavigableMap<RegDate, StatusRegistreIDE> status = new TreeMap<>();
	private NavigableMap<RegDate, TypeOrganisationRegistreIDE> typeOrganisation = new TreeMap<>();
	private NavigableMap<RegDate, AdresseRCEnt> adresseEffective = new TreeMap<>();
	private NavigableMap<RegDate, AdresseRCEnt> adresseBoitePostale = new TreeMap<>();
	private NavigableMap<RegDate, RaisonLiquidationRegistreIDE> raisonDeLiquidation = new TreeMap<>();

	public MockDonneesRegistreIDE() {}

	public MockDonneesRegistreIDE(NavigableMap<RegDate, StatusRegistreIDE> status, NavigableMap<RegDate, TypeOrganisationRegistreIDE> typeOrganisation,
	                              NavigableMap<RegDate, AdresseRCEnt> adresseEffective, NavigableMap<RegDate, AdresseRCEnt> adresseBoitePostale,
	                              NavigableMap<RegDate, RaisonLiquidationRegistreIDE> raisonDeLiquidation) {
		this.status = status;
		this.typeOrganisation = typeOrganisation;
		this.adresseEffective = adresseEffective;
		this.adresseBoitePostale = adresseBoitePostale;
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	@Override
	public List<AdresseRCEnt> getAdresseBoitePostale() {
		return new ArrayList<>(adresseBoitePostale.values());
	}

	public void changeAdresseBoitePostale(RegDate date, AdresseRCEnt nouvelleAdresseBoitePostale) {
		throw new UnsupportedOperationException();
	}

	public void addAdresseBoitePostale(RegDate dateDebut, @Nullable RegDate dateFin, AdresseRCEnt nouvelleAdresseBoitePostale) {
		final Map.Entry<RegDate, AdresseRCEnt> previousEntry = adresseBoitePostale.lastEntry();
		if (previousEntry != null) {
			final AdresseRCEnt previous = previousEntry.getValue();
			adresseBoitePostale.put(previous.getDateDebut(), (new AdresseRCEnt(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getLocalite(), previous.getNumero(),
			                                                             previous.getNumeroAppartement(), previous.getNumeroOrdrePostal(), previous.getNumeroPostal(),
			                                                             previous.getNumeroPostalComplementaire(), previous.getNoOfsPays(), previous.getRue(),
			                                                             previous.getTitre(), previous.getTypeAdresse(), previous.getEgid(), previous.getCasePostale())));
		}
		MockOrganisationHelper.addRangedData(adresseBoitePostale, dateDebut, dateFin, nouvelleAdresseBoitePostale);
	}

	@Override
	public List<AdresseRCEnt> getAdresseEffective() {
		return new ArrayList<>(adresseEffective.values());
	}

	public void changeAdresseEffective(RegDate date, AdresseRCEnt nouvelleAdresseEffective) {
		throw new UnsupportedOperationException();
	}

	public void addAdresseEffective(RegDate dateDebut, @Nullable RegDate dateFin, AdresseRCEnt nouvelleAdresseEffective) {
		final Map.Entry<RegDate, AdresseRCEnt> previousEntry = adresseEffective.lastEntry();
		if (previousEntry != null) {
			final AdresseRCEnt previous = previousEntry.getValue();
			adresseEffective.put(previous.getDateDebut(), (new AdresseRCEnt(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getLocalite(), previous.getNumero(),
			                                                                   previous.getNumeroAppartement(), previous.getNumeroOrdrePostal(), previous.getNumeroPostal(),
			                                                                   previous.getNumeroPostalComplementaire(), previous.getNoOfsPays(), previous.getRue(),
			                                                                   previous.getTitre(), previous.getTypeAdresse(), previous.getEgid(), previous.getCasePostale())));
		}
		MockOrganisationHelper.addRangedData(adresseEffective, dateDebut, dateFin, nouvelleAdresseEffective);
	}

	@Override
	public List<DateRanged<RaisonLiquidationRegistreIDE>> getRaisonDeLiquidation() {
		return MockOrganisationHelper.getHisto(raisonDeLiquidation);
	}

	public void changeRaisonDeLiquidation(RegDate date, RaisonLiquidationRegistreIDE nouvelleRaisonDeLiquidation) {
		MockOrganisationHelper.changeRangedData(raisonDeLiquidation, date, nouvelleRaisonDeLiquidation);
	}

	public void addRaisonDeLiquidation(RegDate date, RaisonLiquidationRegistreIDE nouvelleRaisonDeLiquidation) {
		MockOrganisationHelper.changeRangedData(raisonDeLiquidation, date, nouvelleRaisonDeLiquidation);
	}

	@NotNull
	@Override
	public List<DateRanged<StatusRegistreIDE>> getStatus() {
		return MockOrganisationHelper.getHisto(status);
	}

	@Override
	public StatusRegistreIDE getStatus(RegDate date) {
		return OrganisationHelper.valueForDate(getStatus(), date);
	}

	public void changeStatus(RegDate date, StatusRegistreIDE nouveauStatus) {
		MockOrganisationHelper.changeRangedData(status, date, nouveauStatus);
	}

	public void addStatus(RegDate date, StatusRegistreIDE nouveauStatus) {
		MockOrganisationHelper.changeRangedData(status, date, nouveauStatus);
	}

	@Override
	public List<DateRanged<TypeOrganisationRegistreIDE>> getTypeOrganisation() {
		return MockOrganisationHelper.getHisto(typeOrganisation);
	}

	public void changeTypeOrganisation(RegDate date, TypeOrganisationRegistreIDE nouveauType) {
		MockOrganisationHelper.changeRangedData(typeOrganisation, date, nouveauType);
	}

	public void addTypeOrganisation(RegDate date, TypeOrganisationRegistreIDE nouveauType) {
		MockOrganisationHelper.changeRangedData(typeOrganisation, date, nouveauType);
	}

}

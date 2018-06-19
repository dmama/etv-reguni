package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseBoitePostaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeEntrepriseRegistreIDE;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class MockDonneesRegistreIDE implements DonneesRegistreIDE {

	private NavigableMap<RegDate, StatusRegistreIDE> status = new TreeMap<>();
	private NavigableMap<RegDate, TypeEntrepriseRegistreIDE> typeEntreprise = new TreeMap<>();
	private NavigableMap<RegDate, AdresseEffectiveRCEnt> adresseEffective = new TreeMap<>();
	private NavigableMap<RegDate, AdresseBoitePostaleRCEnt> adresseBoitePostale = new TreeMap<>();
	private NavigableMap<RegDate, RaisonDeRadiationRegistreIDE> raisonDeLiquidation = new TreeMap<>();

	public MockDonneesRegistreIDE() {}

	public MockDonneesRegistreIDE(NavigableMap<RegDate, StatusRegistreIDE> status, NavigableMap<RegDate, TypeEntrepriseRegistreIDE> typeEntreprise,
	                              NavigableMap<RegDate, AdresseEffectiveRCEnt> adresseEffective, NavigableMap<RegDate, AdresseBoitePostaleRCEnt> adresseBoitePostale,
	                              NavigableMap<RegDate, RaisonDeRadiationRegistreIDE> raisonDeLiquidation) {
		this.status = status;
		this.typeEntreprise = typeEntreprise;
		this.adresseEffective = adresseEffective;
		this.adresseBoitePostale = adresseBoitePostale;
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	@Override
	public List<AdresseBoitePostaleRCEnt> getAdresseBoitePostale() {
		return new ArrayList<>(adresseBoitePostale.values());
	}

	public void changeAdresseBoitePostale(RegDate date, AdresseBoitePostaleRCEnt nouvelleAdresseBoitePostale) {
		throw new UnsupportedOperationException();
	}

	public void addAdresseBoitePostale(AdresseBoitePostaleRCEnt nouvelleAdresseBoitePostale) {
		final RegDate dateDebut = nouvelleAdresseBoitePostale.getDateDebut();
		final RegDate dateFin = nouvelleAdresseBoitePostale.getDateFin();

		final Map.Entry<RegDate, AdresseBoitePostaleRCEnt> previousEntry = adresseBoitePostale.lastEntry();
		if (previousEntry != null) {
			final AdresseBoitePostaleRCEnt previous = previousEntry.getValue();
			adresseBoitePostale.put(previous.getDateDebut(), (new AdresseBoitePostaleRCEnt(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getLocalite(), previous.getNumero(),
			                                                                               previous.getNumeroAppartement(), previous.getNumeroOrdrePostal(), previous.getNumeroPostal(),
			                                                                               previous.getNumeroPostalComplementaire(), previous.getNoOfsPays(), previous.getRue(),
			                                                                               previous.getTitre(), previous.getEgid(), previous.getCasePostale())));
		}
		MockEntrepriseHelper.addRangedData(adresseBoitePostale, dateDebut, dateFin, nouvelleAdresseBoitePostale);
	}

	@Override
	public List<AdresseEffectiveRCEnt> getAdresseEffective() {
		return new ArrayList<>(adresseEffective.values());
	}

	public void changeAdresseEffective(RegDate date, AdresseEffectiveRCEnt nouvelleAdresseEffective) {
		throw new UnsupportedOperationException();
	}

	public void addAdresseEffective(AdresseEffectiveRCEnt nouvelleAdresseEffective) {
		final RegDate dateDebut = nouvelleAdresseEffective.getDateDebut();
		final RegDate dateFin = nouvelleAdresseEffective.getDateFin();

		final Map.Entry<RegDate, AdresseEffectiveRCEnt> previousEntry = adresseEffective.lastEntry();
		if (previousEntry != null) {
			final AdresseEffectiveRCEnt previous = previousEntry.getValue();
			adresseEffective.put(previous.getDateDebut(), (new AdresseEffectiveRCEnt(previous.getDateDebut(), dateDebut.getOneDayBefore(), previous.getLocalite(), previous.getNumero(),
			                                                                         previous.getNumeroAppartement(), previous.getNumeroOrdrePostal(), previous.getNumeroPostal(),
			                                                                         previous.getNumeroPostalComplementaire(), previous.getNoOfsPays(), previous.getRue(),
			                                                                         previous.getTitre(), previous.getEgid(), previous.getCasePostale())));
		}
		MockEntrepriseHelper.addRangedData(adresseEffective, dateDebut, dateFin, nouvelleAdresseEffective);
	}

	@Override
	public AdresseEffectiveRCEnt getAdresseEffective(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getAdresseEffective(), date);
	}

	@Override
	public List<DateRanged<RaisonDeRadiationRegistreIDE>> getRaisonDeLiquidation() {
		return MockEntrepriseHelper.getHisto(raisonDeLiquidation);
	}

	public void changeRaisonDeLiquidation(RegDate date, RaisonDeRadiationRegistreIDE nouvelleRaisonDeLiquidation) {
		MockEntrepriseHelper.changeRangedData(raisonDeLiquidation, date, nouvelleRaisonDeLiquidation);
	}

	public void addRaisonDeLiquidation(RegDate dateDebut, RegDate dateFin, RaisonDeRadiationRegistreIDE nouvelleRaisonDeLiquidation) {
		MockEntrepriseHelper.addRangedData(raisonDeLiquidation, dateDebut, dateFin, nouvelleRaisonDeLiquidation);
	}

	@NotNull
	@Override
	public List<DateRanged<StatusRegistreIDE>> getStatus() {
		return MockEntrepriseHelper.getHisto(status);
	}

	@Override
	public StatusRegistreIDE getStatus(RegDate date) {
		return EntrepriseHelper.valueForDate(getStatus(), date);
	}

	public void changeStatus(RegDate date, StatusRegistreIDE nouveauStatus) {
		MockEntrepriseHelper.changeRangedData(status, date, nouveauStatus);
	}

	public void addStatus(RegDate dateDebut, RegDate dateFin, StatusRegistreIDE nouveauStatus) {
		MockEntrepriseHelper.addRangedData(status, dateDebut, dateFin, nouveauStatus);
	}

	@Override
	public List<DateRanged<TypeEntrepriseRegistreIDE>> getTypeEntreprise() {
		return MockEntrepriseHelper.getHisto(typeEntreprise);
	}

	public void changeTypeEntreprise(RegDate date, TypeEntrepriseRegistreIDE nouveauType) {
		MockEntrepriseHelper.changeRangedData(typeEntreprise, date, nouveauType);
	}

	public void addTypeEntreprise(RegDate dateDebut, RegDate dateFin, TypeEntrepriseRegistreIDE nouveauType) {
		MockEntrepriseHelper.addRangedData(typeEntreprise, dateDebut, dateFin, nouveauType);
	}

}

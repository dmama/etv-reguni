package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

/**
 * Représente un object mock pour un site d'organisation. Le mock fait plusieurs choses:
 *
 * - Il rend modifiables les champs de l'entité.
 * - Il implémente éventuellement des mutations spécifiques, nécessaires dans un
 *   contexte de test.
 */
public class MockSiteOrganisation implements SiteOrganisation {

	private final long numeroSite;
	private final NavigableMap<RegDate, String> nom = new TreeMap<>();
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final NavigableMap<RegDate, Integer> siege = new TreeMap<>();
	private final NavigableMap<RegDate, TypeDeSite> typeDeSite = new TreeMap<>();
	private final DonneesRegistreIDE donneesRegistreIDE;
	private final DonneesRC donneesRC;

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

	public void changeSiege(RegDate date, Integer nouveauSiege) {
		MockOrganisationHelper.changeRangedData(siege, date, nouveauSiege);
	}

	public void addSiege(RegDate dateDebut, RegDate dateFin, Integer nouveauSiege) {
		MockOrganisationHelper.addRangedData(siege, dateDebut, dateFin, nouveauSiege);
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
	public List<DateRanged<FonctionOrganisation>> getFonction() {
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
	public DonneesRC getDonneesRC() {
		return donneesRC;
	}

	@Override
	public List<DateRanged<Integer>> getSiege() {
		return MockOrganisationHelper.getHisto(siege);
	}

	@Override
	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return MockOrganisationHelper.getHisto(typeDeSite);
	}
}

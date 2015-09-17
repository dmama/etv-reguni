package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

/**
 * Représente un object mock pour une organisation. Le mock fait plusieurs choses:
 *
 * - Il rend modifiables les champs de l'entité.
 * - Il implémente éventuellement des mutations spécifiques, nécessaires dans un
 *   contexte de test.
 */
public class MockOrganisation implements Organisation {

	private final long idOrganisation;
	private final NavigableMap<RegDate, String> nom = new TreeMap<>();
	private final NavigableMap<RegDate, List<String>> nomsAdditionnels = new TreeMap<>();
	private final NavigableMap<RegDate, FormeLegale> formeLegale = new TreeMap<>();
	private final NavigableMap<RegDate, String> ide = new TreeMap<>();
	private final NavigableMap<RegDate, Long> idSites = new TreeMap<>();
	private final List<MockSiteOrganisation> sites = new ArrayList<>();

	public MockOrganisation(long idOrganisation, RegDate dateDebut, String nom, FormeLegale formeLegale) {
		this.idOrganisation = idOrganisation;
		changeNom(dateDebut, nom);
		changeFormeLegale(dateDebut, formeLegale);
	}

	public void changeNom(RegDate date, String nouveauNom) {
		MockOrganisationHelper.changeRangedData(nom, date, nouveauNom);
	}

	public void addNom(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauNom) {
		MockOrganisationHelper.addRangedData(nom, dateDebut, dateFin, nouveauNom);
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

	public void addNumeroIDE(RegDate dateDebut, @Nullable RegDate dateFin, String nouveauNumeroIDE) {
		MockOrganisationHelper.addRangedData(ide, dateDebut, dateFin, nouveauNumeroIDE);
	}

	public void addSiteId(RegDate dateDebut, @Nullable RegDate dateFin, long idSite) {
		MockOrganisationHelper.addRangedData(idSites, dateDebut, dateFin, idSite);
	}

	public void addDonneesSite(MockSiteOrganisation site) {
		sites.add(site);
	}

	public void addNomsAdditionnels(RegDate dateDebut, RegDate dateFin, String... nouveauxNomsAdditionnels) {
		MockOrganisationHelper.addRangedData(nomsAdditionnels, dateDebut, dateFin, nouveauxNomsAdditionnels != null ? Arrays.asList(nouveauxNomsAdditionnels) : Collections.<String>emptyList());
	}

	@Override
	public long getNumeroOrganisation() {
		return idOrganisation;
	}

	@Override
	public List<SiteOrganisation> getDonneesSites() {
		return new ArrayList<SiteOrganisation>(sites);
	}

	@Override
	public List<DateRanged<Long>> getEnRemplacementDe() {
		return null;
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return MockOrganisationHelper.getHisto(formeLegale);
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return MockOrganisationHelper.getHisto(ide);
	}

	@Override
	public List<DateRanged<String>> getNom() {
		return MockOrganisationHelper.getHisto(nom);
	}

	@Override
	public List<DateRanged<String>> getNomsAdditionels() {
		// un peu de calcul...

		// on commence par regrouper les noms entre eux
		final Map<String, List<DateRange>> noms = new HashMap<>();
		final List<DateRanged<List<String>>> histo = MockOrganisationHelper.getHisto(nomsAdditionnels);
		for (DateRanged<List<String>> range : histo) {
			for (String nom : range.getPayload()) {
				final List<DateRange> rangesPourNom;
				if (!noms.containsKey(nom)) {
					rangesPourNom = new ArrayList<>();
					noms.put(nom, rangesPourNom);
				}
				else {
					rangesPourNom = noms.get(nom);
				}
				rangesPourNom.add(range);
			}
		}

		// puis on reconstitue tous les ranges pour les noms
		final List<DateRanged<String>> result = new ArrayList<>();
		for (Map.Entry<String, List<DateRange>> entry : noms.entrySet()) {
			final List<DateRange> merged = DateRangeHelper.merge(entry.getValue());
			for (DateRange range : merged) {
				result.add(new DateRanged<>(range.getDateDebut(), range.getDateFin(), entry.getKey()));
			}
		}

		// et on trie tout ça
		Collections.sort(result, new DateRangeComparator<>());
		return result;
	}

	@Override
	public List<DateRanged<Long>> getRemplacePar() {
		return null;
	}

	@Override
	public List<DateRanged<Long>> getSites() {
		return MockOrganisationHelper.getHisto(idSites);
	}

	@Override
	public List<DateRanged<Long>> getTransferDe() {
		return null;
	}

	@Override
	public List<DateRanged<Long>> getTransfereA() {
		return null;
	}

	@Override
	public List<DateRanged<Capital>> getCapital() {
		return null;
	}

	@Override
	public List<DateRanged<Integer>> getSiegePrincipal() {
		return null;
	}
}

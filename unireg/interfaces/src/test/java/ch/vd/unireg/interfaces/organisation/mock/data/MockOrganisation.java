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
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

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
	private final List<MockSiteOrganisation> sites = new ArrayList<>();
	private final List<Adresse> adresses = new ArrayList<>();

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

	public void addDonneesSite(MockSiteOrganisation site) {
		sites.add(site);
	}

	public void addNomsAdditionnels(RegDate dateDebut, RegDate dateFin, String... nouveauxNomsAdditionnels) {
		MockOrganisationHelper.addRangedData(nomsAdditionnels, dateDebut, dateFin, nouveauxNomsAdditionnels != null ? Arrays.asList(nouveauxNomsAdditionnels) : Collections.<String>emptyList());
	}

	public void addAdresse(MockAdresse adresse) {
		this.adresses.add(adresse);
		Collections.sort(this.adresses, new DateRangeComparator<>());
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
		throw new NotImplementedException();
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
		throw new NotImplementedException();
	}

	@Override
	public List<DateRanged<Long>> getTransferDe() {
		throw new NotImplementedException();
	}

	@Override
	public List<DateRanged<Long>> getTransfereA() {
		throw new NotImplementedException();
	}

	@Override
	public List<Capital> getCapitaux() {
		throw new NotImplementedException();
	}

	@Override
	public List<Adresse> getAdresses() {
		return adresses;
	}

	@Override
	public List<Siege> getSiegesPrincipaux() {
		final List<Siege> sieges = new ArrayList<>();
		for (MockSiteOrganisation site : sites) {
			for (DateRanged<TypeDeSite> typeSite : site.getTypeDeSite()) {
				if (typeSite.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
					sieges.addAll(site.getSieges());
				}
			}
		}
		Collections.sort(sieges, new DateRangeComparator<>());
		return DateRangeHelper.collate(sieges);
	}
}

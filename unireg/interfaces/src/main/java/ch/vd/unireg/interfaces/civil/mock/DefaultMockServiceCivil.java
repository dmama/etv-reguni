package ch.vd.unireg.interfaces.civil.mock;

import java.util.ArrayList;
import java.util.Collection;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;

public class DefaultMockServiceCivil extends MockServiceCivil {

	private final boolean returnFakeAdresses;

	public DefaultMockServiceCivil() {
		this.returnFakeAdresses = true; // pour des raisons historiques
	}

	public DefaultMockServiceCivil(boolean returnFakeAdresses) {
		this.returnFakeAdresses = returnFakeAdresses;
	}

	/**
	 * Crée des tiers
	 */
	@Override
	protected void init() {
		MockIndividu momo = addIndividu(54321, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
		MockIndividu pierre = addIndividu(12345, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
		MockIndividu bea = addIndividu(23456, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
		MockIndividu julie = addIndividu(6789, RegDate.get(1977, 4, 19), "Goux", "Julie", false);
		MockIndividu sophie = addIndividu(34567, RegDate.get(1964, 4, 8), "Dupuis", "Sophie", false);
		MockIndividu david = addIndividu(45678, RegDate.get(1964, 1, 23), "Dagobert", "David", true);
		MockIndividu julien = addIndividu(56789, RegDate.get(1966, 11, 2), "Martin", "Julien", true);
		MockIndividu leon = addIndividu(78912, RegDate.get(1953, 11, 2), "Dupont", "Léon", true);
		MockIndividu helene = addIndividu(89123, RegDate.get(1963, 8, 20), "Duval", "Hélène", false);
		MockIndividu victor = addIndividu(263343, RegDate.get(1956, 5, 12), "Duplat", "Victor", true);
		addIndividu(123456, RegDate.get(2006, 12, 25), "Petit", "Jésus", true);
		addIndividu(983254, RegDate.get(2005, 3, 30), "Petit", "Enfant", true);
		addIndividu(611836, RegDate.get(1953, 10, 14), "Perroset", "Francis", true); // pour que l'InfoController retourne OK en mode Norentes

		addDefaultAdressesTo(momo);
		addDefaultAdressesTo(pierre);
		addDefaultAdressesTo(bea);
		addDefaultAdressesTo(julie);
		addDefaultAdressesTo(sophie);
		addDefaultAdressesTo(david);
		addDefaultAdressesTo(julien);
		addDefaultAdressesTo(victor);
		addDefaultAdressesTo(helene);

		marieIndividus(momo, bea, RegDate.get(1986, 4, 8));
		marieIndividus(david, julien, RegDate.get(1986, 4, 8));
		marieIndividus(leon, helene, RegDate.get(1986, 4, 8));
		marieIndividu(pierre, RegDate.get(1986, 4, 8));

		addOrigine(bea, MockCommune.Lausanne);
		addNationalite(bea, MockPays.Suisse, RegDate.get(1963, 8, 20), null);
		addPermis(bea, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);

		addOrigine(helene, MockPays.France.getNomMinuscule());
		addNationalite(helene, MockPays.France, RegDate.get(1963, 8, 20), null);
		addPermis(helene, TypePermis.COURTE_DUREE, RegDate.get(1963, 8, 20), null, false);

		addOrigine(sophie, MockPays.Suisse.getNomMinuscule());
		addNationalite(sophie, MockPays.Suisse, RegDate.get(1973, 8, 20), null);
		addPermis(sophie, TypePermis.ETABLISSEMENT, RegDate.get(1973, 8, 20), null, false);

		addOrigine(pierre, MockPays.Suisse.getNomMinuscule());
		addNationalite(pierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null);

		addOrigine(momo, MockPays.Suisse.getNomMinuscule());
		addNationalite(momo, MockPays.Suisse, RegDate.get(1961, 3, 12), null);

		addOrigine(julien, MockPays.Suisse.getNomMinuscule());
		addNationalite(julien, MockPays.Suisse, RegDate.get(1966, 11, 2), null);

		addOrigine(david, MockPays.Suisse.getNomMinuscule());
		addNationalite(david, MockPays.Suisse, RegDate.get(1964, 1, 23), null);

		addOrigine(victor, MockPays.Espagne.getNomMinuscule());
		addNationalite(victor, MockPays.Espagne, RegDate.get(1956, 5, 12), null);

		// Individus du fichier tiers basic
		addDefaultAdressesTo(addIndividu(320073, RegDate.get(1950, 1, 1), "Fuchs", "Philippe", true));
		addDefaultAdressesTo(addIndividu(325740, RegDate.get(1950, 1, 1), "Fuchs", "Nathalie", false));
		addDefaultAdressesTo(addIndividu(327706, RegDate.get(1950, 1, 1), "Lyah", "Emery", true));
		addDefaultAdressesTo(addIndividu(674417, RegDate.get(1950, 1, 1), "Decloux", "Pascaline", true));
		addDefaultAdressesTo(addIndividu(333905, RegDate.get(1950, 1, 1), "Schmid", "Christine", false));
		addDefaultAdressesTo(addIndividu(333908, RegDate.get(1950, 1, 1), "Schmid", "Laurent", true));
		addDefaultAdressesTo(addIndividu(333911, RegDate.get(1950, 1, 1), "Schmid", "Valentin", true));
		addDefaultAdressesTo(addIndividu(325631, RegDate.get(1950, 1, 1), "Emery", "Jean-Christophe", true));
	}

	protected void addDefaultAdressesTo(MockIndividu individu) {
		addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 11, 2), null);
		addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, RegDate.get(1980, 11, 2), null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.interfaces.service.HostCivilService#getAdressesActives(java.lang.Long,
	 *      java.util.Date)
	 */
	@Override
	public Collection<Adresse> getAdressesActives(Long numeroIndividu, RegDate date) {
		
		if (numeroIndividu == 34567 || !returnFakeAdresses) {
			return super.getAdressesActives(numeroIndividu, date);
		}

		Collection<Adresse> adresses = new ArrayList<Adresse>();
		MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
		adresse.setLocalite("Mock Localite " + DateHelper.getCurrentDate());
		adresse.setCasePostale(new CasePostale(TexteCasePostale.CASE_POSTALE, 1234));
		adresse.setNumeroPostal("4848");
		adresse.setDateDebutValidite(RegDate.get(1980, 11, 2));
		adresse.setNumeroOrdrePostal(MockLocalite.Lausanne.getNoOrdre());
		adresse.setCommuneAdresse(MockCommune.Lausanne);
		adresses.add(adresse);

		adresse = new MockAdresse();
		adresse.setTypeAdresse(TypeAdresseCivil.COURRIER);
		adresse.setLocalite("Mock Localite " + DateHelper.getCurrentDate());
		adresse.setCasePostale(new CasePostale(TexteCasePostale.CASE_POSTALE, 4567));
		adresse.setNumeroPostal("5252");
		adresse.setDateDebutValidite(RegDate.get(1980, 11, 2));
		adresse.setNumeroOrdrePostal(MockLocalite.CossonayVille.getNoOrdre());
		adresse.setCommuneAdresse(MockCommune.Cossonay);
		adresses.add(adresse);
		return adresses;
	}

	
}

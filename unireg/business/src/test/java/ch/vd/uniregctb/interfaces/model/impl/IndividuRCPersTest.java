package ch.vd.uniregctb.interfaces.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0010.v4.SwissAddressInformation;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.evd0001.v3.DwellingAddress;
import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.MaritalData;
import ch.vd.evd0001.v3.Residence;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class IndividuRCPersTest extends WithoutSpringTest {

	private ServiceInfrastructureImpl infraService = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService());

	@Test
	public void testGetCelibataire() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final List<MaritalData> statuses = Arrays.asList(celibataire);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEtatCivil(date(1960, 1, 1), null, TypeEtatCivil.CELIBATAIRE, list.get(0));
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2");
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, list.get(1));
	}

	@Test
	public void testGetMariePuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, list.get(2));
	}

	@Test
	public void testGetMariePuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), date(2005, 10, 3), TypeEtatCivil.SEPARE, list.get(2));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.MARIE, list.get(3));
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6");
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, list.get(1));
	}

	@Test
	public void testGetPacsePuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, list.get(2));
	}

	@Test
	public void testGetPacsePuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), date(2005, 10, 3), TypeEtatCivil.PACS_INTERROMPU, list.get(2));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.PACS, list.get(3));
	}

	/**
	 * Un individu peut possèder une séries d'adresses secondaires en parallèle aux adresses principales. La seule contrainte est qu'une adresse secondaire ne peut pas être en même temps sur la même
	 * commune qu'une adresse principale.
	 */
	@Test
	public void initAdressesPrincipalesEtSecondairesMelangees() throws Exception {

		// 3 adresses principales à partir du 1er janvier 1970
		final Residence p1 = newResidencePrincipale(date(1970, 1, 1), null, date(1999, 12, 31), MockRue.CossonayVille.AvenueDuFuniculaire);
		final Residence p2 = newResidencePrincipale(date(1970, 1, 1), date(1982, 1, 1), date(1999, 12, 31), MockRue.CossonayVille.CheminDeRiondmorcel);
		final Residence p3 = newResidencePrincipale(date(2000, 1, 1), null, null, MockRue.Bussigny.RueDeLIndustrie);

		// 2 adresses secondaires à partir du 1er janvier 1970, avec des dates de déménagement entre-mêlées avec celles des adresses principales
		final Residence s1 = newResidenceSecondaire(date(1970, 1, 1), null, null, MockRue.Cully.ChDesColombaires);
		final Residence s2 = newResidenceSecondaire(date(1970, 1, 1), date(1985, 1, 1), null, MockRue.Cully.PlaceDuTemple);

		// initialisation des adresses
		final Collection<Adresse> adresses =
				IndividuRCPers.initAdresses(Collections.<HistoryContact>emptyList(), Arrays.asList(p1, p2, p3, s1, s2), infraService);
		assertNotNull(adresses);
		assertEquals(5, adresses.size());

		final List<Adresse> principales = new ArrayList<Adresse>();
		final List<Adresse> secondaires = new ArrayList<Adresse>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
				secondaires.add(a);
			}
			else {
				fail();
			}
		}

		// les dates de fin des adresses principales ne doivent pas prendre en compte les adresses secondaires
		assertEquals(3, principales.size());
		assertAdresse(date(1970, 1, 1), date(1981, 12, 31), "Avenue du Funiculaire", "Cossonay-Ville", principales.get(0));
		assertAdresse(date(1982, 1, 1), date(1999, 12, 31), "Chemin de Riondmorcel", "Cossonay-Ville", principales.get(1));
		assertAdresse(date(2000, 1, 1), null, "Rue de l'Industrie", "Bussigny-près-Lausanne", principales.get(2));

		// les dates de fin des adresses secondaires ne doivent pas prendre en compte les adresses principales
		assertEquals(2, secondaires.size());
		assertAdresse(date(1970, 1, 1), date(1984, 12, 31), "Ch. des Colombaires", "Cully", secondaires.get(0));
		assertAdresse(date(1985, 1, 1), null, "Place du Temple", "Cully", secondaires.get(1));
	}

	/**
	 * Un individu peut possèder <b>plusieurs</b> séries d'adresses secondaires en parallèle aux adresses principales. La seule contrainte est qu'une adresse secondaire ne peut pas être en même temps sur
	 * la même commune qu'une adresse principale.
	 */
	@Test
	public void initAdressesPrincipalesEtPlusieursSeriesAdressesSecondairesMelangees() throws Exception {

		// 3 adresses principales à partir du 1er janvier 1970
		final Residence p1 = newResidencePrincipale(date(1970, 1, 1), null, date(1999, 12, 31), MockRue.CossonayVille.AvenueDuFuniculaire);
		final Residence p2 = newResidencePrincipale(date(1970, 1, 1), date(1982, 1, 1), date(1999, 12, 31), MockRue.CossonayVille.CheminDeRiondmorcel);
		final Residence p3 = newResidencePrincipale(date(2000, 1, 1), null, null, MockRue.Bussigny.RueDeLIndustrie);

		// 2 adresses secondaires sur Cully à partir du 1er janvier 1970, avec des dates de déménagement entre-mêlées avec celles des adresses principales
		final Residence s1 = newResidenceSecondaire(date(1970, 1, 1), null, null, MockRue.Cully.ChDesColombaires);
		final Residence s2 = newResidenceSecondaire(date(1970, 1, 1), date(1985, 1, 1), null, MockRue.Cully.PlaceDuTemple);

		// 2 adresses secondaires sur Epesse à partir du 1er janvier 1970, avec des dates de déménagement entre-mêlées avec celles des adresses principales
		final Residence z1 = newResidenceSecondaire(date(1970, 1, 1), null, date(1981, 12, 31), MockRue.Epesses.LaPlace);
		final Residence z2 = newResidenceSecondaire(date(1970, 1, 1), date(1978, 1, 1), date(1981, 12, 31), MockRue.Epesses.ChDuMont);

		// initialisation des adresses
		final Collection<Adresse> adresses =
				IndividuRCPers.initAdresses(Collections.<HistoryContact>emptyList(), Arrays.asList(p1, p2, p3, s1, s2, z1, z2), infraService);
		assertNotNull(adresses);
		assertEquals(7, adresses.size());

		final List<Adresse> principales = new ArrayList<Adresse>();
		final List<Adresse> secondaires = new ArrayList<Adresse>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
				secondaires.add(a);
			}
			else {
				fail();
			}
		}

		// les dates de fin des adresses principales ne doivent pas prendre en compte les adresses secondaires
		assertEquals(3, principales.size());
		assertAdresse(date(1970, 1, 1), date(1981, 12, 31), "Avenue du Funiculaire", "Cossonay-Ville", principales.get(0));
		assertAdresse(date(1982, 1, 1), date(1999, 12, 31), "Chemin de Riondmorcel", "Cossonay-Ville", principales.get(1));
		assertAdresse(date(2000, 1, 1), null, "Rue de l'Industrie", "Bussigny-près-Lausanne", principales.get(2));

		// les séries (= une série par commune) d'adresses secondaires ne doivent pas interagir entres-elles.
		assertEquals(4, secondaires.size());
		assertAdresse(date(1970, 1, 1), date(1977, 12, 31), "La Place", "Epesses", secondaires.get(0));
		assertAdresse(date(1970, 1, 1), date(1984, 12, 31), "Ch. des Colombaires", "Cully", secondaires.get(1));
		assertAdresse(date(1978, 1, 1), date(1981, 12, 31), "Ch. du Mont", "Epesses", secondaires.get(2));
		assertAdresse(date(1985, 1, 1), null, "Place du Temple", "Cully", secondaires.get(3));
	}

	private static void assertAdresse(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String rue, @Nullable String localite, Adresse adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateFin, adresse.getDateFin());
		assertEquals(rue, adresse.getRue());
		assertEquals(localite, adresse.getLocalite());
	}

	private static Residence newResidencePrincipale(RegDate arrivalDate, @Nullable RegDate movingDate, @Nullable RegDate departureDate, MockRue rue) {
		Commune commune = rue.getLocalite().getCommuneLocalite();
		final SwissMunicipality municipality = newSwissMunicipality(commune);
		final DwellingAddress dwellingAddress = newDwellingAddress(movingDate, rue);
		return new Residence(new Residence.MainResidence(), null, null, municipality, municipality, XmlUtils.regdate2xmlcal(arrivalDate), null, dwellingAddress, XmlUtils.regdate2xmlcal(
				departureDate), null);
	}

	private static Residence newResidenceSecondaire(RegDate arrivalDate, @Nullable RegDate movingDate, @Nullable RegDate departureDate, MockRue rue) {
		Commune commune = rue.getLocalite().getCommuneLocalite();
		final SwissMunicipality municipality = newSwissMunicipality(commune);
		final DwellingAddress dwellingAddress = newDwellingAddress(movingDate, rue);
		return new Residence(null, null, new Residence.OtherResidence(), municipality, municipality, XmlUtils.regdate2xmlcal(arrivalDate), null, dwellingAddress, XmlUtils.regdate2xmlcal(
				departureDate), null);
	}

	private static SwissMunicipality newSwissMunicipality(Commune commune) {
		return new SwissMunicipality(commune.getNoOFS(), commune.getNomMinuscule(), EchHelper.sigleCantonToAbbreviation(commune.getSigleCanton()), null);
	}

	private static DwellingAddress newDwellingAddress(RegDate movingDate, MockRue rue) {
		DwellingAddress address = new DwellingAddress();
		address.setMovingDate(XmlUtils.regdate2xmlcal(movingDate));
		address.setAddress(newSwissAddressInformation(rue));
		return address;
	}

	private static SwissAddressInformation newSwissAddressInformation(MockRue rue) {
		SwissAddressInformation info = new SwissAddressInformation();
		info.setStreet(rue.getDesignationCourrier());
		info.setSwissZipCodeId(rue.getLocalite().getNPA());
		info.setTown(rue.getLocalite().getNomCompletMinuscule());
		return info;
	}

	private static void assertEtatCivil(RegDate dateDebut, @Nullable RegDate dateFin, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(dateFin, etat.getDateFin());
		assertEquals(type, etat.getTypeEtatCivil());
	}

	private static MaritalData newMaritalData(RegDate date, String type) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		return data;
	}

	private static MaritalData newMaritalData(RegDate date, String type, RegDate separation) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);

		final MaritalData.Separation sep = new MaritalData.Separation();
		sep.setDateOfSeparation(XmlUtils.regdate2xmlcal(separation));
		data.getSeparation().add(sep);

		return data;
	}

	private MaritalData newMaritalData(RegDate date, String type, RegDate separation, RegDate reconciliation) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);

		final MaritalData.Separation sep = new MaritalData.Separation();
		sep.setDateOfSeparation(XmlUtils.regdate2xmlcal(separation));
		sep.setSeparationTill(XmlUtils.regdate2xmlcal(reconciliation));
		data.getSeparation().add(sep);

		return data;
	}
}

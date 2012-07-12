package ch.vd.unireg.interfaces.civil.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0010.v4.SwissAddressInformation;
import ch.ech.ech0044.v2.NamedPersonId;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.evd0001.v3.DwellingAddress;
import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Identity;
import ch.vd.evd0001.v3.MaritalData;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.PersonIdentification;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0001.v3.Residence;
import ch.vd.evd0001.v3.ResidencePermit;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IndividuRCPersTest extends WithoutSpringTest {

	private ServiceInfrastructureRaw infraService = new DefaultMockServiceInfrastructureService();

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
				IndividuRCPers.initAdresses(null, Collections.<HistoryContact>emptyList(), Arrays.asList(p1, p2, p3, s1, s2), infraService);
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
				IndividuRCPers.initAdresses(null, Collections.<HistoryContact>emptyList(), Arrays.asList(p1, p2, p3, s1, s2, z1, z2), infraService);
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

	@Test
	public void testGetPersonWithHistoryValues() throws Exception {

		final Person person = newPerson(123345L, "Jean", "Rucher", date(1965, 3, 12), Sexe.MASCULIN);
		// les adresses
		person.getContactHistory().add(newHistoryContact(date(1965, 3, 12), date(1983, 7, 4), MockRue.Cully.ChDesColombaires));
		person.getContactHistory().add(newHistoryContact(date(1983, 7, 5), null, MockRue.Chamblon.RueDesUttins));
		person.getResidenceHistory().add(newResidencePrincipale(date(1965, 3, 12), null, date(1983, 7, 4), MockRue.Cully.ChDesColombaires));
		person.getResidenceHistory().add(newResidencePrincipale(date(1983, 7, 5), null, null, MockRue.Chamblon.RueDesUttins));

		// les états-civils
		person.getMaritalStatusHistory().add(newMaritalData(date(1965, 3, 12), TypeEtatCivil.CELIBATAIRE));
		person.getMaritalStatusHistory().add(newMaritalData(date(1989, 5, 1), TypeEtatCivil.MARIE));

		// les permis
		person.getResidencePermitHistory().add(newResidencePermit(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT));

		// les relations
		final List<Relationship> relations = new ArrayList<Relationship>();
		relations.add(newRelation(date(1989, 5, 1), null, 562841L, TypeRelation.VERS_CONJOINT));

		// on vérifie que les valeurs historisées sont bien lues
		final Individu ind = IndividuRCPers.get(person, relations, true, infraService);
		assertNotNull(ind);
		assertEquals(123345L, ind.getNoTechnique());
		assertEquals("Jean", ind.getPrenom());
		assertEquals("Rucher", ind.getNom());
		assertEquals(date(1965, 3, 12), ind.getDateNaissance());
		assertTrue(ind.isSexeMasculin());

		final Collection<Adresse> adresses = ind.getAdresses();
		assertNotNull(adresses);
		assertEquals(4, adresses.size());

		final List<Adresse> courriers = new ArrayList<Adresse>();
		final List<Adresse> principales = new ArrayList<Adresse>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
				courriers.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
		}

		assertEquals(2, courriers.size());
		assertAdresse(date(1965, 3, 12), date(1983, 7, 4), "Ch. des Colombaires", "Cully", courriers.get(0));
		assertAdresse(date(1983, 7, 5), null, "Rue des Uttins", "Chamblon", courriers.get(1));

		assertEquals(2, principales.size());
		assertAdresse(date(1965, 3, 12), date(1983, 7, 4), "Ch. des Colombaires", "Cully", principales.get(0));
		assertAdresse(date(1983, 7, 5), null, "Rue des Uttins", "Chamblon", principales.get(1));

		final EtatCivilList etatCivils = ind.getEtatsCivils();
		assertNotNull(etatCivils);
		assertEquals(2, etatCivils.size());
		assertEtatCivil(date(1965, 3, 12), date(1989, 4, 30), TypeEtatCivil.CELIBATAIRE, etatCivils.get(0));
		assertEtatCivil(date(1989, 5, 1), null, TypeEtatCivil.MARIE, etatCivils.get(1));

		final PermisList permis = ind.getPermis();
		assertNotNull(permis);
		assertEquals(1, permis.size());
		assertPermis(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT, permis.get(0));
	}

	/**
	 * [SIFISC-5181] Ce test s'assure que les valeurs courantes sont bien prises en compte en mode non-historique.
	 */
	@Test
	public void testGetPersonWithCurrentValues() throws Exception {

		final Person person = newPerson(123345L, "Jean", "Rucher", date(1965, 3, 12), Sexe.MASCULIN);
		// les adresses courantes
		person.setCurrentContact(newMailAddress(MockRue.Chamblon.RueDesUttins));
		person.getCurrentResidence().add(newResidencePrincipale(date(1983, 7, 5), null, null, MockRue.Chamblon.RueDesUttins));

		// l'état-civil courant
		person.setCurrentMaritalStatus(newMaritalData(date(1989, 5, 1), TypeEtatCivil.MARIE));

		// le permis courant
		person.setCurrentResidencePermit(newResidencePermit(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT));

		// les relations courantes
		final List<Relationship> relations = new ArrayList<Relationship>();
		relations.add(newRelation(date(1989, 5, 1), null, 562841L, TypeRelation.VERS_CONJOINT));

		// on vérifie que les valeurs courantes sont bien lues
		final Individu ind = IndividuRCPers.get(person, relations, false, infraService);
		assertNotNull(ind);
		assertEquals(123345L, ind.getNoTechnique());
		assertEquals("Jean", ind.getPrenom());
		assertEquals("Rucher", ind.getNom());
		assertEquals(date(1965, 3, 12), ind.getDateNaissance());
		assertTrue(ind.isSexeMasculin());

		final Collection<Adresse> adresses = ind.getAdresses();
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		final List<Adresse> courriers = new ArrayList<Adresse>();
		final List<Adresse> principales = new ArrayList<Adresse>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
				courriers.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
		}

		assertEquals(1, courriers.size());
		assertAdresse(null, null, "Rue des Uttins", "Chamblon", courriers.get(0));

		assertEquals(1, principales.size());
		assertAdresse(date(1983, 7, 5), null, "Rue des Uttins", "Chamblon", principales.get(0));

		final EtatCivilList etatCivils = ind.getEtatsCivils();
		assertNotNull(etatCivils);
		assertEquals(1, etatCivils.size());
		assertEtatCivil(date(1989, 5, 1), null, TypeEtatCivil.MARIE, etatCivils.get(0));

		final PermisList permis = ind.getPermis();
		assertNotNull(permis);
		assertEquals(1, permis.size());
		assertPermis(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT, permis.get(0));
	}

	private static void assertPermis(RegDate dateDebut, @Nullable RegDate dateFin, TypePermis typePermis, Permis permis) {
		assertNotNull(permis);
		assertEquals(dateDebut, permis.getDateDebut());
		assertEquals(dateFin, permis.getDateFin());
		assertEquals(typePermis, permis.getTypePermis());
	}

	private enum TypeRelation {

		VERS_CONJOINT("1"),
		VERS_PARTENAIRE_ENREGISTRE("2"),
		VERS_MERE("3"),
		VERS_PERE("4"),
		VERS_FILLE("101"),
		VERS_FILS("102");

		private final String echCode;

		TypeRelation(String echCode) {
			this.echCode = echCode;
		}

		public String getEchCode() {
			return echCode;
		}
	}

	private static Relationship newRelation(RegDate dateDebut, @Nullable RegDate dateFin, long noIndividuLie, TypeRelation typeRelation) {
		final Relationship relation = new Relationship();
		relation.setRelationValidFrom(XmlUtils.regdate2xmlcal(dateDebut));
		relation.setRelationValidFrom(XmlUtils.regdate2xmlcal(dateFin));
		relation.setLocalPersonId(new NamedPersonId("ct.vd.rcpers", String.valueOf(noIndividuLie)));
		relation.setTypeOfRelationship(typeRelation.getEchCode());
		return relation;
	}

	private static ResidencePermit newResidencePermit(RegDate dateDebut, @Nullable RegDate dateFin, TypePermis typePermis) {
		final ResidencePermit permit = new ResidencePermit();
		permit.setResidencePermitValidFrom(XmlUtils.regdate2xmlcal(dateDebut));
		permit.setResidencePermitTill(XmlUtils.regdate2xmlcal(dateFin));
		permit.setResidencePermit(TypePermis.toEch(typePermis));
		return permit;
	}

	private static HistoryContact newHistoryContact(RegDate dateDebut, @Nullable RegDate dateFin, MockRue rue) {
		final HistoryContact contactHistory = new HistoryContact();
		final MailAddress mailAddress = newMailAddress(rue);
		contactHistory.setContact(mailAddress);
		contactHistory.setContactValidFrom(XmlUtils.regdate2xmlcal(dateDebut));
		contactHistory.setContactValidTill(XmlUtils.regdate2xmlcal(dateFin));
		return contactHistory;
	}

	private static MailAddress newMailAddress(MockRue rue) {
		final MailAddress mailAddress = new MailAddress();
		mailAddress.setAddressInformation(newAddressInformation(rue));
		return mailAddress;
	}

	private static Person newPerson(long noInd, String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		final Person person = new Person();
		final Identity identity = new Identity();
		identity.setCallName(prenom);
		final PersonIdentification identification = new PersonIdentification();
		identification.setLocalPersonId(new NamedPersonId("ch.vd.rcpers", String.valueOf(noInd)));
		identification.setOfficialName(nom);
		identification.setDateOfBirth(EchHelper.partialDateToEch44(dateNaissance));
		identification.setSex(EchHelper.sexeToEch44(sexe));
		identity.setPersonIdentification(identification);
		person.setIdentity(identity);
		return person;
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
		final DwellingAddress address = new DwellingAddress();
		address.setMovingDate(XmlUtils.regdate2xmlcal(movingDate));
		address.setAddress(newSwissAddressInformation(rue));
		return address;
	}

	private static SwissAddressInformation newSwissAddressInformation(MockRue rue) {
		final SwissAddressInformation info = new SwissAddressInformation();
		info.setStreet(rue.getDesignationCourrier());
		info.setSwissZipCode(rue.getLocalite().getNPA());
		info.setSwissZipCodeId(rue.getLocalite().getNoOrdre());
		info.setTown(rue.getLocalite().getNomCompletMinuscule());
		return info;
	}

	private static AddressInformation newAddressInformation(MockRue rue) {
		AddressInformation info = new AddressInformation();
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

	private static MaritalData newMaritalData(RegDate date, TypeEtatCivil etatCivil) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(EchHelper.etatCivilToEch11(etatCivil));
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

	@Test
	public void testInitPrenom() throws Exception {
		assertNull(IndividuRCPers.initPrenom(null, null));
		assertNull(IndividuRCPers.initPrenom("", ""));
		assertNull(IndividuRCPers.initPrenom(" ", "  "));
		assertEquals("Paul", IndividuRCPers.initPrenom("Paul", null));
		assertEquals("Paul", IndividuRCPers.initPrenom("Paul", "Jacques Paul Henri"));
		assertEquals("Paul", IndividuRCPers.initPrenom("Paul", "Jacques Paul Henri"));
		assertEquals("Jacques", IndividuRCPers.initPrenom(null, "Jacques"));
		assertEquals("Jacques", IndividuRCPers.initPrenom(null, "Jacques Paul Henri"));
		assertEquals("Jacques", IndividuRCPers.initPrenom("  ", "Jacques Paul Henri"));
		assertEquals("Jacques-Martin", IndividuRCPers.initPrenom(null, "Jacques-Martin Paul Henri"));
		assertEquals("Jacques-Martin", IndividuRCPers.initPrenom(null, " Jacques-Martin Paul Henri"));
	}
}

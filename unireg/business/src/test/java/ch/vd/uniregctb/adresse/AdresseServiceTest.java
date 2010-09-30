package ch.vd.uniregctb.adresse;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.validation.ValidationInterceptor;

import static ch.vd.uniregctb.adresse.AdresseTestCase.assertAdresse;
import static ch.vd.uniregctb.adresse.AdresseTestCase.assertAdressesEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class AdresseServiceTest extends BusinessTest {

	private AdresseService adresseService;
	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private ValidationInterceptor validationInterceptor;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");

		// Pas d'indexation parce qu'on teste des cas qui font peter l'indexation
		// et qui pourrissent les logs!
		globalTiersIndexer.setOnTheFlyIndexation(false);

		// Instanciation du service à la main pour pouvoir taper dans les méthodes protégées.
		adresseService = new AdresseServiceImpl(tiersService, tiersDAO, serviceInfra, servicePM, serviceCivil);
	}

	@Override
	public void onTearDown() throws Exception {
		globalTiersIndexer.setOnTheFlyIndexation(true);
		super.onTearDown();
	}

	@Test
	public void testGetAdressesFiscalesNonHabitantAvecDomicileSeulement() throws Exception {

		PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenom("Gengis");
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2003, 1, 3));
			adresse.setUsage(TypeAdresseTiers.DOMICILE);
			adresse.setNumeroMaison("6C");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		tiersDAO.save(nonhabitant);

		AdressesFiscales adrs = adresseService.getAdressesFiscales(nonhabitant, null, false);
		assertNotNull(adrs.courrier);
		assertNotNull(adrs.domicile);
		assertNotNull(adrs.poursuite);
		assertNotNull(adrs.representation);

		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseFiscale.COURRIER, null, false));
		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseFiscale.DOMICILE, null, false));
		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseFiscale.POURSUITE, null, false));
		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseFiscale.REPRESENTATION, null, false));
	}

	@Test
	public void testGetAdressesFiscalesHistoNonHabitantAvecDomicileSeulement() throws Exception {

		final RegDate dateDebut1 = date(2003, 1, 3);
		final RegDate dateFin1 = date(2006, 5, 5);
		final RegDate dateDebut2 = date(2007, 11, 1);

		PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenom("Gengis");
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(dateDebut1);
			adresse.setDateFin(dateFin1);
			adresse.setUsage(TypeAdresseTiers.DOMICILE);
			adresse.setNumeroMaison("6C");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(dateDebut2);
			adresse.setUsage(TypeAdresseTiers.DOMICILE);
			adresse.setNumeroMaison("6C");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		tiersDAO.save(nonhabitant);

		AdressesFiscalesHisto adrs = adresseService.getAdressesFiscalHisto(nonhabitant, false);
		assertNotNull(adrs.courrier);
		assertNotNull(adrs.domicile);
		assertNotNull(adrs.poursuite);
		assertNotNull(adrs.representation);

		assertEquals(dateDebut1, adrs.courrier.get(0).getDateDebut());
		assertEquals(dateFin1, adrs.courrier.get(0).getDateFin());
		assertEquals(dateDebut2, adrs.courrier.get(1).getDateDebut());
		assertNull(adrs.courrier.get(1).getDateFin());
		assertEquals(dateDebut1, adrs.domicile.get(0).getDateDebut());
		assertNull(adrs.courrier.get(1).getDateFin());
		assertEquals(dateDebut1, adrs.poursuite.get(0).getDateDebut());
		assertNull(adrs.courrier.get(1).getDateFin());
		assertEquals(dateDebut1, adrs.representation.get(0).getDateDebut());
		assertNull(adrs.courrier.get(1).getDateFin());
	}

	@Test
	public void testGetAdressesFiscalesHistoSansTiers() throws Exception {
		assertNull(adresseService.getAdressesFiscales(null, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseFiscale.COURRIER, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseFiscale.DOMICILE, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseFiscale.POURSUITE, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseFiscale.REPRESENTATION, date(2000, 1, 1), false));
	}

	@Test
	public void testGetAdressesFiscalesHistoSansAdresseFiscale() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1,
						1), date(1987, 12, 11));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						date(1987, 12, 12), date(2001, 6, 3));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null,
						date(2001, 6, 4), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980,
						1, 1), date(1987, 12, 11));
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						date(1987, 12, 12), null);
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(3, adresses.courrier.size());
		assertAdressesEquals(adresses.courrier, adresses.representation);
		assertEquals(2, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(1980, 1, 1), courrier1.getDateDebut());
		assertEquals(date(1987, 12, 11), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(1987, 12, 12), courrier2.getDateDebut());
		assertEquals(date(2001, 6, 3), courrier2.getDateFin());
		assertEquals("Cossonay-Ville", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique courrier3 = adresses.courrier.get(2);
		assertEquals(date(2001, 6, 4), courrier3.getDateDebut());
		assertNull(courrier3.getDateFin());
		assertEquals("Clées, Les", courrier3.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier3.getSource());
		assertFalse(courrier3.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(1980, 1, 1), poursuite1.getDateDebut());
		assertEquals(date(1987, 12, 11), poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());

		final AdresseGenerique poursuite2 = adresses.poursuite.get(1);
		assertEquals(date(1987, 12, 12), poursuite2.getDateDebut());
		assertNull(poursuite2.getDateFin());
		assertEquals("Cossonay-Ville", poursuite2.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite2.getSource());
		assertFalse(poursuite2.isDefault());
	}

	@Test
	public void testGetAdressesFiscalesSansAdresseCivile() throws Exception {

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});

		// Crée un non-habitant (= n'existe pas dans le registre civil)
		PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenom("Gengis");
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1974, 1, 3));
			adresse.setDateFin(date(1980, 1, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("6C");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1980, 2, 1));
			adresse.setDateFin(date(1998, 4, 22));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("11B");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeMarcelin.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1998, 4, 23));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("2");
			adresse.setNumeroRue(MockRue.CossonayVille.CheminDeRiondmorcel.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.CossonayVille.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1974, 1, 3));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.POURSUITE);
			adresse.setNumeroMaison("1");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		tiersDAO.save(nonhabitant);

		// Vérification des adresses histo
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(nonhabitant, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(1974, 1, 3), date(1980, 1, 31), "Lausanne", Source.FISCALE, false, adresses.courrier.get(0));
			assertEquals("6C", adresses.courrier.get(0).getNumero());
			assertAdresse(date(1980, 2, 1), date(1998, 4, 22), "Lausanne", Source.FISCALE, false, adresses.courrier.get(1));
			assertEquals("11B", adresses.courrier.get(1).getNumero());
			assertAdresse(date(1998, 4, 23), null, "Cossonay-Ville", Source.FISCALE, false, adresses.courrier.get(2));
			assertEquals("2", adresses.courrier.get(2).getNumero());

			assertEquals(3, adresses.representation.size());
			assertAdresse(date(1974, 1, 3), date(1980, 1, 31), "Lausanne", Source.FISCALE, true, adresses.representation.get(0));
			assertEquals("6C", adresses.representation.get(0).getNumero());
			assertAdresse(date(1980, 2, 1), date(1998, 4, 22), "Lausanne", Source.FISCALE, true, adresses.representation.get(1));
			assertEquals("11B", adresses.representation.get(1).getNumero());
			assertAdresse(date(1998, 4, 23), null, "Cossonay-Ville", Source.FISCALE, true, adresses.representation.get(2));
			assertEquals("2", adresses.representation.get(2).getNumero());

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, false, adresses.poursuite.get(0));
			assertEquals("1", adresses.poursuite.get(0).getNumero());

			assertEquals(1, adresses.domicile.size());
			assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, true, adresses.domicile.get(0));
			assertEquals("1", adresses.domicile.get(0).getNumero());
		}

		// Vérification des adresses ponctuelles
		{

			{
				final AdressesFiscales adresses = adresseService.getAdressesFiscales(nonhabitant, date(1980, 1, 1), false);
				assertNotNull(adresses);

				assertAdresse(date(1974, 1, 3), date(1980, 1, 31), "Lausanne", Source.FISCALE, false, adresses.courrier);
				assertEquals("6C", adresses.courrier.getNumero());
				assertAdresse(date(1974, 1, 3), date(1980, 1, 31), "Lausanne", Source.FISCALE, true, adresses.representation);
				assertEquals("6C", adresses.representation.getNumero());
				assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, false, adresses.poursuite);
				assertEquals("1", adresses.poursuite.getNumero());
				assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, true, adresses.domicile);
				assertEquals("1", adresses.domicile.getNumero());

				assertAdressesByTypeEquals(adresses, nonhabitant, date(1980, 1, 1));
			}

			{
				final AdressesFiscales adresses = adresseService.getAdressesFiscales(nonhabitant, date(1980, 7, 1), false);
				assertNotNull(adresses);

				assertAdresse(date(1980, 2, 1), date(1998, 4, 22), "Lausanne", Source.FISCALE, false, adresses.courrier);
				assertEquals("11B", adresses.courrier.getNumero());
				assertAdresse(date(1980, 2, 1), date(1998, 4, 22), "Lausanne", Source.FISCALE, true, adresses.representation);
				assertEquals("11B", adresses.representation.getNumero());
				assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, false, adresses.poursuite);
				assertEquals("1", adresses.poursuite.getNumero());
				assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, true, adresses.domicile);
				assertEquals("1", adresses.domicile.getNumero());

				assertAdressesByTypeEquals(adresses, nonhabitant, date(1980, 7, 1));
			}

			{
				final AdressesFiscales adresses = adresseService.getAdressesFiscales(nonhabitant, date(1998, 7, 1), false);
				assertNotNull(adresses);

				assertAdresse(date(1998, 4, 23), null, "Cossonay-Ville", Source.FISCALE, false, adresses.courrier);
				assertEquals("2", adresses.courrier.getNumero());
				assertAdresse(date(1998, 4, 23), null, "Cossonay-Ville", Source.FISCALE, true, adresses.representation);
				assertEquals("2", adresses.representation.getNumero());
				assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, false, adresses.poursuite);
				assertEquals("1", adresses.poursuite.getNumero());
				assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, true, adresses.domicile);
				assertEquals("1", adresses.domicile.getNumero());

				assertAdressesByTypeEquals(adresses, nonhabitant, date(1998, 7, 1));
			}
		}
	}

	@Test
	public void testGetAdressesFiscalesSansAdresseCivile2() throws Exception {

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});

		// Crée un non-habitant (= n'existe pas dans le registre civil)
		PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenom("Gengis");
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1974, 1, 3));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("6C");
			adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1974, 1, 3));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.REPRESENTATION);
			adresse.setNumeroMaison("2");
			adresse.setNumeroRue(MockRue.CossonayVille.CheminDeRiondmorcel.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.CossonayVille.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1974, 1, 3));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.POURSUITE);
			adresse.setNumeroMaison("1");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(1974, 1, 3));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.DOMICILE);
			adresse.setNumeroMaison("11B");
			adresse.setNumeroRue(MockRue.LesClees.ChampDuRaffour.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.LesClees.getNoOrdre());
			nonhabitant.addAdresseTiers(adresse);
		}
		tiersDAO.save(nonhabitant);

		// Vérification des adresses histo
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(nonhabitant, false);
			assertNotNull(adresses);

			assertEquals(1, adresses.courrier.size());
			assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, false, adresses.courrier.get(0));
			assertEquals("6C", adresses.courrier.get(0).getNumero());

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(1974, 1, 3), null, "Cossonay-Ville", Source.FISCALE, false, adresses.representation.get(0));
			assertEquals("2", adresses.representation.get(0).getNumero());

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(1974, 1, 3), null, "Bex", Source.FISCALE, false, adresses.poursuite.get(0));
			assertEquals("1", adresses.poursuite.get(0).getNumero());

			assertEquals(1, adresses.domicile.size());
			assertAdresse(date(1974, 1, 3), null, "Clées, Les", Source.FISCALE, false, adresses.domicile.get(0));
			assertEquals("11B", adresses.domicile.get(0).getNumero());
		}

		// Vérification des adresses ponctuelles
		{
			final AdressesFiscales adresses = adresseService.getAdressesFiscales(nonhabitant, date(1980, 1, 1), false);
			assertNotNull(adresses);

			assertAdresse(date(1974, 1, 3), null, "Lausanne", Source.FISCALE, false, adresses.courrier);
			assertEquals("6C", adresses.courrier.getNumero());
			assertAdresse(date(1974, 1, 3), null, "Cossonay-Ville", Source.FISCALE, false, adresses.representation);
			assertEquals("2", adresses.representation.getNumero());
			assertAdresse(date(1974, 1, 3), null, "Bex", Source.FISCALE, false, adresses.poursuite);
			assertEquals("1", adresses.poursuite.getNumero());
			assertAdresse(date(1974, 1, 3), null, "Clées, Les", Source.FISCALE, false, adresses.domicile);
			assertEquals("11B", adresses.domicile.getNumero());

			assertAdressesByTypeEquals(adresses, nonhabitant, date(1980, 1, 1));
		}
	}

	/**
	 * Cas général de plusieurs adresses civiles surchargées par plusieurs adresses fiscales.
	 *
	 * <pre>
	 *                       +-----------------------------------------+---------------------------------------------------+----------------------------+---------------------------------------------------------------------------------
	 * Adresses civiles:     | Lausanne                                ¦ Cossonay                                          | Les Clees                  | Lausanne
	 *                       +-----------------------------------------+---------------------------------------------------+----------------------------+---------------------------------------------------------------------------------
	 *                       ¦- 2000.01.01                 2000.09.19 -¦- 2000.09.20                           2002.02.27 -¦- 2002.02.28    2002.03.14 -¦- 2002.03.15
	 *                       ¦
	 *                       ¦                            +----------------------------+                            +-----------------------------------------+                            +----------------------------+
	 * Adresses fiscales:    ¦                            | Bex                        |                            | Romainmotier                            |                            | Bex                        |
	 *                       ¦                            +----------------------------+                            +-----------------------------------------+                            +----------------------------+
	 *                       ¦                            ¦- 2000.03.20    2001.12.31 -¦                            ¦- 2002.01.10                 2002.03.31 -¦                            ¦- 2002.06.01    2002.07.31 -¦
	 *                       ¦                            ¦                            ¦                            ¦                                         ¦                            ¦                            ¦
	 *                       +----------------------------+----------------------------+----------------------------+-----------------------------------------+----------------------------+----------------------------+-----------------
	 * Adresses résultantes: | Lausanne                   ¦ Bex                        | Cossonay                   | Romainmotier                            | Lausanne                   | Bex                        | Lausanne
	 *                       +----------------------------+----------------------------+----------------------------+-----------------------------------------+----------------------------+----------------------------+-----------------
	 *                       ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20    2001.12.31 -¦- 2002.01.01    2002.01.09 -¦- 2002.01.10                 2002.03.31 -¦- 2002.04.01    2002.05.31 -¦- 2002.06.01    2002.07.31 -¦- 2002.08.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCasGeneral() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), date(2000, 9, 19));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(2000, 9, 20), date(2002, 2, 27));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null,
						date(2002, 2, 28), date(2002, 3, 14));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2002, 3,
						15), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000,
						1, 1), null);
			}
		});

		// Crée un habitant avec trois adresses fiscales surchargées
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(date(2001, 12, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("3");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2002, 1, 10));
			adresse.setDateFin(date(2002, 3, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("1");
			adresse.setNumeroRue(MockRue.Romainmotier.CheminDuCochet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Romainmotier.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2002, 6, 1));
			adresse.setDateFin(date(2002, 7, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("2");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(7, adresses.courrier.size());
		assertEquals(4, adresses.representation.size());
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertEquals(date(2000, 3, 19), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
		assertEquals(date(2001, 12, 31), courrier2.getDateFin());
		assertEquals("Bex", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique courrier3 = adresses.courrier.get(2);
		assertEquals(date(2002, 1, 1), courrier3.getDateDebut());
		assertEquals(date(2002, 1, 9), courrier3.getDateFin());
		assertEquals("Cossonay-Ville", courrier3.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier3.getSource());
		assertFalse(courrier3.isDefault());

		final AdresseGenerique courrier4 = adresses.courrier.get(3);
		assertEquals(date(2002, 1, 10), courrier4.getDateDebut());
		assertEquals(date(2002, 3, 31), courrier4.getDateFin());
		assertEquals("Romainmôtier", courrier4.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier4.getSource());
		assertFalse(courrier4.isDefault());

		final AdresseGenerique courrier5 = adresses.courrier.get(4);
		assertEquals(date(2002, 4, 1), courrier5.getDateDebut());
		assertEquals(date(2002, 5, 31), courrier5.getDateFin());
		assertEquals("Lausanne", courrier5.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier5.getSource());
		assertFalse(courrier5.isDefault());

		final AdresseGenerique courrier6 = adresses.courrier.get(5);
		assertEquals(date(2002, 6, 1), courrier6.getDateDebut());
		assertEquals(date(2002, 7, 31), courrier6.getDateFin());
		assertEquals("Bex", courrier6.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier6.getSource());
		assertFalse(courrier6.isDefault());

		final AdresseGenerique courrier7 = adresses.courrier.get(6);
		assertEquals(date(2002, 8, 1), courrier7.getDateDebut());
		assertNull(courrier7.getDateFin());
		assertEquals("Lausanne", courrier7.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier7.getSource());
		assertFalse(courrier7.isDefault());

		final AdresseGenerique representation1 = adresses.representation.get(0);
		assertEquals(date(2000, 1, 1), representation1.getDateDebut());
		assertEquals(date(2000, 9, 19), representation1.getDateFin());
		assertEquals("Lausanne", representation1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
		assertFalse(representation1.isDefault());

		final AdresseGenerique representation2 = adresses.representation.get(1);
		assertEquals(date(2000, 9, 20), representation2.getDateDebut());
		assertEquals(date(2002, 2, 27), representation2.getDateFin());
		assertEquals("Cossonay-Ville", representation2.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation2.getSource());
		assertFalse(representation2.isDefault());

		final AdresseGenerique representation3 = adresses.representation.get(2);
		assertEquals(date(2002, 2, 28), representation3.getDateDebut());
		assertEquals(date(2002, 3, 14), representation3.getDateFin());
		assertEquals("Clées, Les", representation3.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation3.getSource());
		assertFalse(representation3.isDefault());

		final AdresseGenerique representation4 = adresses.representation.get(3);
		assertEquals(date(2002, 3, 15), representation4.getDateDebut());
		assertNull(representation4.getDateFin());
		assertEquals("Lausanne", representation4.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation4.getSource());
		assertFalse(representation4.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	@Test
	public void testGetAdressesFiscales_JIRA_2856() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu vanessa = addIndividu(noIndividu, date(1987, 8, 7), "Richard", "Vanessa", false);

				// adresses courriers
				addAdresse(vanessa, EnumTypeAdresse.COURRIER, MockRue.Orbe.RueDuMoulinet, null,
						date(2009, 10, 1), null);

				// adresses principales/poursuite
				addAdresse(vanessa, EnumTypeAdresse.PRINCIPALE, MockRue.Orbe.RueDuMoulinet, null, date(2009, 12, 1), null);
			}
		});

		// Crée un habitant avec un adresse fiscale 'courrier' surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
		
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2010, 6, 23));
			adresse.setDateFin(date(2010, 9, 15));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("1");
			adresse.setNumeroRue(MockRue.Lausanne.PlaceSaintFrancois.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
			habitant.addAdresseTiers(adresse);

		}

		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(habitant,null,false);
		assertNotNull(adresses);

	

		final AdresseGenerique courrier1 = adresses.courrier;
		assertEquals(date(2010, 9, 16), courrier1.getDateDebut());
		assertEquals("Orbe", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());


	}

	/**
	 * Cas général d'une adresse civile unique surchargée par une adresse fiscale, mais pointant sur une autre adresse civile.
	 *
	 * <pre>
	 *                       +--------------------------------------------------------------------------
	 * Adresses civiles:     | Lausanne
	 *                       +--------------------------------------------------------------------------
	 *                       ¦- 2000.01.01
	 *                       ¦
	 *                       ¦                            +----------------------------+
	 * Adresses fiscales:    ¦                            | Bex                        |
	 *                       ¦                            +----------------------------+
	 *                       ¦                            ¦- 2000.03.20    2001.12.31 -¦
	 *                       ¦                            ¦                            ¦
	 *                       +----------------------------+----------------------------+----------------
	 * Adresses résultantes: | Lausanne                   | Bex                        | Lausanne
	 *                       +----------------------------+----------------------------+----------------
	 *                       ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20    2001.12.31 -¦- 2002.01.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoSurchargeCivil() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);
			}
		});

		// Crée un habitant avec un adresse fiscale 'courrier' surchargée pointant vers l'adresse civile 'principale'
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseCivile adresse = new AdresseCivile();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(date(2001, 12, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setType(EnumTypeAdresse.PRINCIPALE);
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(3, adresses.courrier.size());
		assertEquals(1, adresses.representation.size());
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertEquals(date(2000, 3, 19), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
		assertEquals(date(2001, 12, 31), courrier2.getDateFin());
		assertEquals("Bex", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique courrier3 = adresses.courrier.get(2);
		assertEquals(date(2002, 1, 1), courrier3.getDateDebut());
		assertNull(courrier3.getDateFin());
		assertEquals("Lausanne", courrier3.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier3.getSource());
		assertFalse(courrier3.isDefault());

		final AdresseGenerique representation1 = adresses.representation.get(0);
		assertEquals(date(2000, 1, 1), representation1.getDateDebut());
		assertNull(representation1.getDateFin());
		assertEquals("Lausanne", representation1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
		assertFalse(representation1.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Bex", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	/**
	 * Cas général d'une adresse civile unique surchargée par une adresse fiscale, mais pointant sur l'adresse d'un autre tiers.
	 *
	 * <pre>
	 *                                   +--------------------------------------------------------------------------
	 * Adresses civiles:                 | Lausanne
	 *                                   +--------------------------------------------------------------------------
	 *                                   ¦- 2000.01.01
	 *                                   ¦
	 *                                   +--------------------------------------------------------------------------
	 * Adresses civiles autre tiers:     | Bex
	 *                                   +--------------------------------------------------------------------------
	 *                                   ¦- 2000.01.01
	 *                                   ¦
	 *                                   ¦                            +----------------------------+
	 * Adresses fiscales:                ¦                            | Bex                        |
	 *                                   ¦                            +----------------------------+
	 *                                   ¦                            ¦- 2000.03.20    2001.12.31 -¦
	 *                                   ¦                            ¦                            ¦
	 *                                   +----------------------------+----------------------------+----------------
	 * Adresses résultantes:             | Lausanne                   | Bex                        | Lausanne
	 *                                   +----------------------------+----------------------------+----------------
	 *                                   ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20    2001.12.31 -¦- 2002.01.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoSurchargeAutreTiers() throws Exception {
		final long noIndividu = 1;
		final long noAutreIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, date(2000, 1,
						1), null);

				MockIndividu pierre = addIndividu(noAutreIndividu, date(1953, 11, 2), "Dubois", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);
			}
		});

		// Crée l'autre habitant
		PersonnePhysique autreHabitant = new PersonnePhysique(true);
		autreHabitant.setNumeroIndividu(noAutreIndividu);
		autreHabitant = (PersonnePhysique) tiersDAO.save(autreHabitant);

		// Crée un habitant avec un adresse fiscale 'courrier' surchargée pointant vers l'adresse 'courrier' d'un autre habitant
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseAutreTiers adresse = new AdresseAutreTiers();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(date(2001, 12, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setType(TypeAdresseTiers.COURRIER);
			adresse.setAutreTiersId(autreHabitant.getId());
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);
		hibernateTemplate.flush();

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(3, adresses.courrier.size());
		assertEquals(1, adresses.representation.size());
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertEquals(date(2000, 3, 19), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
		assertEquals(date(2001, 12, 31), courrier2.getDateFin());
		assertEquals("Bex", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique courrier3 = adresses.courrier.get(2);
		assertEquals(date(2002, 1, 1), courrier3.getDateDebut());
		assertNull(courrier3.getDateFin());
		assertEquals("Lausanne", courrier3.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier3.getSource());
		assertFalse(courrier3.isDefault());

		final AdresseGenerique representation1 = adresses.representation.get(0);
		assertEquals(date(2000, 1, 1), representation1.getDateDebut());
		assertNull(representation1.getDateFin());
		assertEquals("Lausanne", representation1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
		assertFalse(representation1.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	/**
	 * Cas particulier d'une adresse civile unique surchargée plusieurs fois par des adresses fiscales.
	 *
	 * <pre>
	 *                       +-------------------------------------------------------------------------------------------------------------------------------------------------
	 * Adresses civiles:     | Lausanne
	 *                       +-------------------------------------------------------------------------------------------------------------------------------------------------
	 *                       ¦- 2000.01.01
	 *                       ¦
	 *                       ¦                            +----------------------------+                            +-----------------------------------------+
	 * Adresses fiscales:    ¦                            | Bex                        |                            | Romainmotier                            |
	 *                       ¦                            +----------------------------+                            +-----------------------------------------+
	 *                       ¦                            ¦- 2000.03.20    2001.12.31 -¦                            ¦- 2002.01.10                 2002.03.31 -¦
	 *                       ¦                            ¦                            ¦                            ¦                                         ¦
	 *                       +----------------------------+----------------------------+----------------------------+-----------------------------------------+----------------
	 * Adresses résultantes: | Lausanne                   | Bex                        | Lausanne                   | Romainmotier                            | Lausanne
	 *                       +----------------------------+----------------------------+----------------------------+-----------------------------------------+----------------
	 *                       ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20    2001.12.31 -¦- 2002.01.01    2002.01.09 -¦- 2002.01.10                 2002.03.31 -¦- 2002.04.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCasParticulier1() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, date(2000, 1,
						1), null);
			}
		});

		// Crée un habitant avec deux adresses fiscales surchargées
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(date(2001, 12, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("3");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2002, 1, 10));
			adresse.setDateFin(date(2002, 3, 31));
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("1");
			adresse.setNumeroRue(MockRue.Romainmotier.CheminDuCochet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Romainmotier.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(5, adresses.courrier.size());
		assertEquals(1, adresses.representation.size());
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertEquals(date(2000, 3, 19), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
		assertEquals(date(2001, 12, 31), courrier2.getDateFin());
		assertEquals("Bex", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique courrier3 = adresses.courrier.get(2);
		assertEquals(date(2002, 1, 1), courrier3.getDateDebut());
		assertEquals(date(2002, 1, 9), courrier3.getDateFin());
		assertEquals("Lausanne", courrier3.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier3.getSource());
		assertFalse(courrier3.isDefault());

		final AdresseGenerique courrier4 = adresses.courrier.get(3);
		assertEquals(date(2002, 1, 10), courrier4.getDateDebut());
		assertEquals(date(2002, 3, 31), courrier4.getDateFin());
		assertEquals("Romainmôtier", courrier4.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier4.getSource());
		assertFalse(courrier4.isDefault());

		final AdresseGenerique courrier5 = adresses.courrier.get(4);
		assertEquals(date(2002, 4, 1), courrier5.getDateDebut());
		assertNull(courrier5.getDateFin());
		assertEquals("Lausanne", courrier5.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier5.getSource());
		assertFalse(courrier5.isDefault());

		final AdresseGenerique representation1 = adresses.representation.get(0);
		assertEquals(date(2000, 1, 1), representation1.getDateDebut());
		assertNull(representation1.getDateFin());
		assertEquals("Lausanne", representation1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
		assertFalse(representation1.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	/**
	 * Cas particulier d'une adresse civile unique surchargée par une adresse fiscale ouverte.
	 *
	 * <pre>
	 *                       +---------------------------------------------
	 * Adresses civiles:     | Lausanne
	 *                       +---------------------------------------------
	 *                       ¦- 2000.01.01
	 *                       ¦
	 *                       ¦                            +----------------
	 * Adresses fiscales:    ¦                            | Bex
	 *                       ¦                            +----------------
	 *                       ¦                            ¦- 2000.03.20
	 *                       ¦                            ¦
	 *                       +----------------------------+----------------
	 * Adresses résultantes: | Lausanne                   | Bex
	 *                       +----------------------------+----------------
	 *                       ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCasParticulier2() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("3");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(2, adresses.courrier.size());
		assertEquals(1, adresses.representation.size());
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertEquals(date(2000, 3, 19), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
		assertNull(courrier2.getDateFin());
		assertEquals("Bex", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique representation1 = adresses.representation.get(0);
		assertEquals(date(2000, 1, 1), representation1.getDateDebut());
		assertNull(representation1.getDateFin());
		assertEquals("Lausanne", representation1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
		assertFalse(representation1.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	/**
	 * Cas particulier de deux adresses civiles surchargées par une adresse fiscale ouverte.
	 *
	 * <pre>
	 *                       +-----------------------------------+-------------------
	 * Adresses civiles:     | Lausanne                          | Cossonay-Ville
	 *                       +-----------------------------------+-------------------
	 *                       ¦- 2000.01.01          2000.07.12 - ¦- 2000.07.13
	 *                       ¦
	 *                       ¦                            +--------------------------
	 * Adresses fiscales:    ¦                            | Bex
	 *                       ¦                            +--------------------------
	 *                       ¦                            ¦- 2000.03.20
	 *                       ¦                            ¦
	 *                       +----------------------------+--------------------------
	 * Adresses résultantes: | Lausanne                   | Bex
	 *                       +----------------------------+--------------------------
	 *                       ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCasParticulier3() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), date(2000, 7, 12));
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(2000, 7, 13), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("3");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adresses);
		assertEquals(2, adresses.courrier.size());
		assertEquals(2, adresses.representation.size());
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertEquals(date(2000, 3, 19), courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique courrier2 = adresses.courrier.get(1);
		assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
		assertNull(courrier2.getDateFin());
		assertEquals("Bex", courrier2.getLocalite());
		assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
		assertFalse(courrier2.isDefault());

		final AdresseGenerique representation1 = adresses.representation.get(0);
		assertEquals(date(2000, 1, 1), representation1.getDateDebut());
		assertEquals(date(2000, 7, 12), representation1.getDateFin());
		assertEquals("Lausanne", representation1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
		assertFalse(representation1.isDefault());

		final AdresseGenerique representation2 = adresses.representation.get(1);
		assertEquals(date(2000, 7, 13), representation2.getDateDebut());
		assertNull(representation2.getDateFin());
		assertEquals("Cossonay-Ville", representation2.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, representation2.getSource());
		assertFalse(representation2.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	/**
	 * Cas particulier d'adresses civiles avec deux adresses principales actives en même temps
	 * <p/>
	 * <pre>
	 *                       +-----------------------------------+
	 * Adresses civiles:     | Lausanne                          |
	 *                       +-----------------------------------+
	 *                       ¦- 2000.01.01          2000.07.12 - ¦
	 *                       ¦
	 *                       ¦                            +--------------------------
	 *                       ¦                            | Bex
	 *                       ¦                            +--------------------------
	 *                       ¦                            ¦- 2000.03.20
	 *                       ¦                            ¦
	 *                       +----------------------------+--------------------------
	 * Adresses résultantes: | Lausanne                   | Bex
	 *                       +----------------------------+--------------------------
	 *                       ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoDonneesIncoherentesPlusieursPrincipales() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), date(2000, 7, 12));
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null,
						date(2000, 3, 20), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		tiersDAO.save(habitant);

		// Vérification des adresses - non-strict
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
			assertNotNull(adresses);
			assertEquals(2, adresses.courrier.size());

			final AdresseGenerique courrier0 = adresses.courrier.get(0);
			assertEquals(date(2000, 1, 1), courrier0.getDateDebut());
			assertEquals(date(2000, 3, 19), courrier0.getDateFin());
			assertEquals("Lausanne", courrier0.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, courrier0.getSource());
			assertFalse(courrier0.isDefault());

			final AdresseGenerique courrier1 = adresses.courrier.get(1);
			assertEquals(date(2000, 3, 20), courrier1.getDateDebut());
			assertNull(courrier1.getDateFin());
			assertEquals("Bex", courrier1.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
			assertFalse(courrier1.isDefault());
		}

		// Vérification des adresses - strict
		{
			try {
				adresseService.getAdressesFiscalHisto(habitant, true);
				fail();
			}
			catch (AdresseException e) {
				assertEquals("L'adresse civile courrier qui commence le 2000.03.20 et finit le null chevauche l'adresse précédente qui commence le 2000.01.01 et finit le 2000.07.12", e.getMessage());
			}
		}
	}

	/**
	 * Cas particulier d'adresses civiles avec une adresse principales ayant une date de début après la date de fin
	 * <p/>
	 * <pre>
	 *                       +-----------------------------------+
	 * Adresses civiles:     | Lausanne                          |
	 *                       +-----------------------------------+
	 *                       ¦- 2020.01.01          2000.03.19 - ¦
	 *
	 *                                                    +--------------------------
	 *                                                    | Bex
	 *                                                    +--------------------------
	 *                                                    ¦- 2000.03.20
	 *                                                    ¦
	 *                                                    +--------------------------
	 * Adresses résultantes:                              | Bex
	 *                                                    +--------------------------
	 *                                                    ¦- 2000.03.20
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoDonneesIncoherentesDatesDebutEtFinInversees() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2020, 1, 1), date(2000, 3, 19));
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null,
						date(2000, 3, 20), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		tiersDAO.save(habitant);

		// Vérification des adresses - non-strict
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
			assertNotNull(adresses);
			assertEquals(1, adresses.courrier.size());

			final AdresseGenerique courrier0 = adresses.courrier.get(0);
			assertEquals(date(2000, 3, 20), courrier0.getDateDebut());
			assertNull(courrier0.getDateFin());
			assertEquals("Bex", courrier0.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, courrier0.getSource());
			assertFalse(courrier0.isDefault());
		}

		// Vérification des adresses - strict
		{
			try {
				adresseService.getAdressesFiscalHisto(habitant, true);
				fail();
			}
			catch (AdresseException e) {
				assertEquals("adresse civile courrier :\nLa date de début [01.01.2020] ne peut pas être après la date de fin [19.03.2000].", e.getMessage());
			}
		}
	}

	/**
	 * Cas particulier d'une adresse civile unique surchargée par une adresse fiscale annulée.
	 * <p>
	 * [UNIREG-888] les adresses annulées sont maintenant retournées telles quelles.
	 *
	 * <pre>
	 *                       +---------------------------------------------
	 * Adresses civiles:     | Lausanne
	 *                       +---------------------------------------------
	 *                       ¦- 2000.01.01
	 *                       ¦
	 *                       ¦                            +----------------
	 * Adresses fiscales:    ¦                            | Bex *ANNULEE*
	 *                       ¦                            +----------------
	 *                       ¦                            ¦- 2000.03.20
	 *
	 *                       +---------------------------------------------
	 * Adresses résultantes: | Lausanne
	 *                       +---------------------------------------------
	 *                       ¦- 2000.01.01                ¦
	 *                       ¦                            +----------------
	 *                       ¦                            | Bex *ANNULEE*
	 *                       ¦                            +----------------
	 *                       ¦                            ¦- 2000.03.20
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesAvecAdresseAnnulee() throws Exception {
		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setNumeroMaison("3");
			adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
			adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
			adresse.setAnnule(true);
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		/*
		 * Vérification des adresses historiques
		 */
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
			assertNotNull(adresses);
			assertEquals(2, adresses.courrier.size());
			assertEquals(1, adresses.representation.size());
			assertEquals(1, adresses.poursuite.size());
			assertAdressesEquals(adresses.poursuite, adresses.domicile);

			final AdresseGenerique courrier1 = adresses.courrier.get(0);
			assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
			assertNull(courrier1.getDateFin());
			assertEquals("Lausanne", courrier1.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
			assertFalse(courrier1.isDefault());
			assertFalse(courrier1.isAnnule());

			final AdresseGenerique courrier2 = adresses.courrier.get(1);
			assertEquals(date(2000, 3, 20), courrier2.getDateDebut());
			assertNull(courrier2.getDateFin());
			assertEquals("Bex", courrier2.getLocalite());
			assertEquals(AdresseGenerique.Source.FISCALE, courrier2.getSource());
			assertFalse(courrier2.isDefault());
			assertTrue(courrier2.isAnnule());

			final AdresseGenerique representation1 = adresses.representation.get(0);
			assertEquals(date(2000, 1, 1), representation1.getDateDebut());
			assertNull(representation1.getDateFin());
			assertEquals("Lausanne", representation1.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, representation1.getSource());
			assertFalse(representation1.isDefault());
			assertFalse(representation1.isAnnule());

			final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
			assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
			assertNull(poursuite1.getDateFin());
			assertEquals("Lausanne", poursuite1.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
			assertFalse(poursuite1.isDefault());
			assertFalse(poursuite1.isAnnule());
		}

		/*
		 * Vérification des adresse ponctuelles
		 */
		{
			// avant le début de l'adresse fiscale annulée
			final AdressesFiscales adresses = adresseService.getAdressesFiscales(habitant, null, false);

			final AdresseGenerique courrier = adresses.courrier;
			assertEquals(date(2000, 1, 1), courrier.getDateDebut());
			assertNull(courrier.getDateFin());
			assertEquals("Lausanne", courrier.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, courrier.getSource());
			assertFalse(courrier.isDefault());

			final AdresseGenerique representation = adresses.representation;
			assertEquals(date(2000, 1, 1), representation.getDateDebut());
			assertNull(representation.getDateFin());
			assertEquals("Lausanne", representation.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, representation.getSource());
			assertFalse(representation.isDefault());

			final AdresseGenerique poursuite = adresses.poursuite;
			assertEquals(date(2000, 1, 1), poursuite.getDateDebut());
			assertNull(poursuite.getDateFin());
			assertEquals("Lausanne", poursuite.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, poursuite.getSource());
			assertFalse(poursuite.isDefault());

			assertAdressesByTypeEquals(adresses, habitant, null);
		}

		{
			// après le début de l'adresse fiscale annulée
			final AdressesFiscales adresses = adresseService.getAdressesFiscales(habitant, date(2000, 4, 1), false);

			final AdresseGenerique courrier = adresses.courrier;
			assertEquals(date(2000, 1, 1), courrier.getDateDebut());
			assertNull(courrier.getDateFin());
			assertEquals("Lausanne", courrier.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, courrier.getSource());
			assertFalse(courrier.isDefault());

			final AdresseGenerique representation = adresses.representation;
			assertEquals(date(2000, 1, 1), representation.getDateDebut());
			assertNull(representation.getDateFin());
			assertEquals("Lausanne", representation.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, representation.getSource());
			assertFalse(representation.isDefault());

			final AdresseGenerique poursuite = adresses.poursuite;
			assertEquals(date(2000, 1, 1), poursuite.getDateDebut());
			assertNull(poursuite.getDateFin());
			assertEquals("Lausanne", poursuite.getLocalite());
			assertEquals(AdresseGenerique.Source.CIVILE, poursuite.getSource());
			assertFalse(poursuite.isDefault());

			assertAdressesByTypeEquals(adresses, habitant, date(2000, 4, 1));
		}
	}

	/**
	 * Cas particulier du ménage.
	 */
	@Test
	public void testGetAdressesFiscalesHistoMenageCommun() throws Exception {
		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);

				MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);

				// adresses courriers
				addAdresse(virginie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002,
						2, 2), null);

				// adresses principales/poursuite
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002,
						2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));
			}
		});

		// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
		final long noMenageCommun = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, date(2004, 7, 14), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				tiersService.addTiersToCouple(menage, conjoint, date(2004, 7, 14), null);
				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adresses);
		assertEquals(1, adresses.courrier.size());
		assertAdressesEquals(adresses.courrier, adresses.representation);
		assertEquals(1, adresses.poursuite.size());
		assertAdressesEquals(adresses.poursuite, adresses.domicile);

		final AdresseGenerique courrier1 = adresses.courrier.get(0);
		assertEquals(date(2000, 1, 1), courrier1.getDateDebut());
		assertNull(courrier1.getDateFin());
		assertEquals("Lausanne", courrier1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, courrier1.getSource());
		assertFalse(courrier1.isDefault());

		final AdresseGenerique poursuite1 = adresses.poursuite.get(0);
		assertEquals(date(2000, 1, 1), poursuite1.getDateDebut());
		assertNull(poursuite1.getDateFin());
		assertEquals("Lausanne", poursuite1.getLocalite());
		assertEquals(AdresseGenerique.Source.CIVILE, poursuite1.getSource());
		assertFalse(poursuite1.isDefault());
	}

	/**
	 * Cas particulier du ménage composé de non-habitants (donc sans adresses civiles) et avec un principal qui possède une adresse courrier
	 * définie au niveau fiscal.
	 */
	@Test
	public void testGetAdressesFiscalesHistoMenageCommunNonHabitants() throws Exception {

		// Crée un ménage composé de deux non-habitants et un principal avec une adresse fiscale courrier

		final long noMenageCommun = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique principal = new PersonnePhysique(false);
				principal.setPrenom("Tommy");
				principal.setNom("Zrwg");
				principal.setSexe(Sexe.MASCULIN);
				principal.setDateNaissance(date(1977, 1, 1));

				AdresseSuisse adresse = new AdresseSuisse();
				adresse.setDateDebut(date(1980, 1, 1));
				adresse.setNumeroRue(MockRue.Lausanne.RouteMaisonNeuve.getNoRue());
				adresse.setNumeroMaison("12");
				adresse.setUsage(TypeAdresseTiers.COURRIER);
				principal.addAdresseTiers(adresse);

				PersonnePhysique conjoint = new PersonnePhysique(false);
				conjoint.setPrenom("Lolo");
				conjoint.setNom("Zrwg");
				conjoint.setSexe(Sexe.FEMININ);
				conjoint.setDateNaissance(date(1978, 1, 1));

				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, date(2004, 7, 14), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				tiersService.addTiersToCouple(menage, conjoint, date(2004, 7, 14), null);
				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		/*
		 * Vérification des adresses historiques
		 */
		{
			// Vérification des adresses
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(menage, false);
			assertNotNull(adresses);
			assertEquals(1, adresses.courrier.size());
			assertEquals(1, adresses.representation.size());
			assertEquals(1, adresses.poursuite.size());
			assertEquals(1, adresses.domicile.size());

			final AdresseGenerique courrier = adresses.courrier.get(0);
			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, false, courrier);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), courrier.getNumeroRue());

			final AdresseGenerique representation = adresses.representation.get(0);
			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, representation);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), representation.getNumeroRue());

			final AdresseGenerique poursuite = adresses.poursuite.get(0);
			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, poursuite);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), poursuite.getNumeroRue());

			final AdresseGenerique domicile = adresses.domicile.get(0);
			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, domicile);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), domicile.getNumeroRue());
		}

		/*
		 * Vérification des adresses ponctuelles
		 */
		{
			// Vérification des adresses
			final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, date(2008, 1, 1), false);
			assertNotNull(adresses);

			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, false, adresses.courrier);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), adresses.courrier.getNumeroRue());

			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, adresses.representation);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), adresses.representation.getNumeroRue());

			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, adresses.poursuite);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), adresses.poursuite.getNumeroRue());

			assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, adresses.domicile);
			assertEquals(MockRue.Lausanne.RouteMaisonNeuve.getNoRue(), adresses.domicile.getNumeroRue());

			assertAdressesByTypeEquals(adresses, menage, date(2008, 1, 1));
		}
	}

	/**
	 * Cas général d'une personne avec une adresse civile unique non surchargée, mais sous tutelle pendant une certaine période.
	 *
	 * <pre>
	 *                               +--------------------------------------------------------------------------
	 * Adresse civile pupille:       | Lausanne
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse prof. tuteur:         | Cossonay-Ville
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 1985.04.01
	 *                               ¦
	 *                               ¦                            +----------------------------+
	 * Rapport-entre-tiers:          ¦                            | Tutelle                    |
	 *                               ¦                            +----------------------------+
	 *                               ¦                            ¦- 2004.01.01    2007.12.31 -¦
	 *                               ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------
	 * Adresse courrier résultante:  | Lausanne                   | Cossonay-Ville             | Lausanne
	 *                               +----------------------------+----------------------------+----------------
	 *                               ¦- 2000.01.01    2003.12.31 -¦- 2004.01.01    2007.12.31 -¦- 2008.01.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoTutelle() throws Exception {
		final long noPupille = 2;
		final long noTuteur = 5;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// la pupille
				MockIndividu paul = addIndividu(noPupille, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);

				// le tuteur
				MockIndividu jean = addIndividu(noTuteur, date(1966, 4, 2), "Dupneu", "Jean", true);

				// adresses courriers
				addAdresse(jean, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);

				// adresses principales/poursuite
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);
			}
		});

		final long numeroContribuablePupille = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée le pupille et le tuteru
				PersonnePhysique pupille = new PersonnePhysique(true);
				pupille.setNumeroIndividu(noPupille);
				pupille = (PersonnePhysique) tiersDAO.save(pupille);

				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// Crée la tutelle proprement dites
				RapportEntreTiers rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 1, 1));
				rapport.setDateFin(date(2007, 12, 31));
				rapport.setObjet(tuteur);
				rapport.setSujet(pupille);
				tiersDAO.save(rapport);

				return pupille.getNumero();
			}
		});

		// Vérification des adresses
		{
			final Tiers pupille = tiersService.getTiers(numeroContribuablePupille);
			assertNotNull(pupille);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(pupille, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2003, 12, 31), "Lausanne", Source.CIVILE, false, adresses.courrier.get(0));
			assertAdresse(date(2004, 1, 1), date(2007, 12, 31), "Cossonay-Ville", Source.TUTELLE, false, adresses.courrier.get(1));
			assertAdresse(date(2008, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.courrier.get(2));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	/**
	 * Cas d'un couple avec la personne principale sous tutelle pendant une certaine période.
	 *
	 * <pre>
	 *                               +--------------------------------------------------------------------------
	 * Adresse civile principal:     | Lausanne
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse civile conjoint:      | Bex
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse prof. tuteur:         | Cossonay-Ville
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 1985.04.01
	 *                               ¦
	 *                               ¦                            +----------------------------+
	 * Tutelle du principal:         ¦                            | Tutelle                    |
	 *                               ¦                            +----------------------------+
	 *                               ¦                            ¦- 2004.01.01    2007.12.31 -¦
	 *                               ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------
	 * Adresse pupille résultante:   | Lausanne                   | Cossonay-Ville             | Lausanne
	 *                               +----------------------------+----------------------------+----------------
	 *                               ¦- 2000.01.01    2003.12.31 -¦- 2004.01.01    2007.12.31 -¦- 2008.01.01
	 *                               ¦                            ¦                            ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse conjoint résultante:  | Bex                        ¦                            ¦
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01                ¦                            ¦
	 *                               ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------
	 * Adresse ménage résultante:    | Lausanne                   | Bex                        | Lausanne
	 *                               +----------------------------+----------------------------+----------------
	 *                               ¦- 2000.01.01    2003.12.31 -¦- 2004.01.01    2007.12.31 -¦- 2008.01.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCoupleTutellePrincipal() throws Exception {
		final long noPrincipal = 2;
		final long noConjoint = 3;
		final long noTuteur = 5;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// la pupille
				MockIndividu paul = addIndividu(noPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);

				// le conjoint
				MockIndividu jeanne = addIndividu(noConjoint, date(1954, 11, 2), "Dupont", "Jeanne", false);
				addAdresse(jeanne, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);
				marieIndividus(paul, jeanne, date(2000, 1, 1));

				// le tuteur
				MockIndividu jean = addIndividu(noTuteur, date(1966, 4, 2), "Dupneu", "Jean", true);
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);
			}
		});

		final class Numeros {
			long numeroContribuablePrincipal;
			long numeroContribuableConjoint;
			long numeroContribuableMenage;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée le ménage
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noConjoint);
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noPrincipal);

				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, date(2000, 1, 1), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				numeros.numeroContribuableMenage = menage.getNumero();
				principal = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				numeros.numeroContribuablePrincipal = principal.getNumero();

				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2000, 1, 1), null);
				conjoint = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				numeros.numeroContribuableConjoint = conjoint.getNumero();

				// Crée le tuteur
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// Crée la tutelle proprement dites
				rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 1, 1));
				rapport.setDateFin(date(2007, 12, 31));
				rapport.setObjet(tuteur);
				rapport.setSujet(principal);
				tiersDAO.save(rapport);
				return null;
			}
		});

		// Vérification des adresses du principal
		{
			final Tiers principal = tiersService.getTiers(numeros.numeroContribuablePrincipal);
			assertNotNull(principal);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(principal, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2003, 12, 31), "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2004, 1, 1), date(2007, 12, 31), "Cossonay-Ville", Source.TUTELLE, false, adresses.courrier.get(1));
			assertAdresse(date(2008, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier.get(2));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}

		// Vérification des adresses du conjoint
		{
			final Tiers conjoint = tiersService.getTiers(numeros.numeroContribuableConjoint);
			assertNotNull(conjoint);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(conjoint, false);
			assertNotNull(adresses);

			assertEquals(1, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, true, adresses.courrier.get(0));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}

		// Vérification des adresses du ménage
		{
			final Tiers menage = tiersService.getTiers(numeros.numeroContribuableMenage);
			assertNotNull(menage);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(menage, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2003, 12, 31), "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2004, 1, 1), date(2007, 12, 31), "Bex", Source.CONJOINT, false, adresses.courrier.get(1));
			assertAdresse(date(2008, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier.get(2));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	/**
	 * Cas d'un couple avec le conjoint sous tutelle pendant une certaine période.
	 *
	 * <pre>
	 *                               +--------------------------------------------------------------------------
	 * Adresse civile principal:     | Lausanne
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse civile conjoint:      | Bex
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse prof. tuteur:         | Cossonay-Ville
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 1985.04.01
	 *                               ¦
	 *                               ¦                            +----------------------------+
	 * Tutelle du conjoint:          ¦                            | Tutelle                    |
	 *                               ¦                            +----------------------------+
	 *                               ¦                            ¦- 2004.01.01    2007.12.31 -¦
	 *                               ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------
	 * Adresse principal résultante: | Lausanne
	 *                               +----------------------------+----------------------------+----------------
	 *                               ¦- 2000.01.01                ¦                            ¦
	 *                               ¦                            ¦                            ¦
	 *                               +--------------------------------------------------------------------------
	 * Adresse conjoint résultante:  | Bex                        ¦ Cossonay-Ville             ¦ Bex
	 *                               +--------------------------------------------------------------------------
	 *                               ¦- 2000.01.01    2003.12.31 -¦- 2004.01.01    2007.12.31 -¦- 2008.01.01
	 *                               ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------
	 * Adresse ménage résultante:    | Lausanne
	 *                               +----------------------------+----------------------------+----------------
	 *                               ¦- 2000.01.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCoupleTutelleConjoint() throws Exception {
		final long noPrincipal = 2;
		final long noConjoint = 3;
		final long noTuteur = 5;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// le principal
				MockIndividu paul = addIndividu(noPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);

				// le conjoint
				MockIndividu jeanne = addIndividu(noConjoint, date(1954, 11, 2), "Dupont", "Jeanne", false);
				addAdresse(jeanne, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);
				marieIndividus(paul, jeanne, date(2000, 1, 1));

				// le tuteur
				MockIndividu jean = addIndividu(noTuteur, date(1966, 4, 2), "Dupneu", "Jean", true);
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);
			}
		});

		final class Numeros {
			long numeroContribuablePrincipal;
			long numeroContribuableConjoint;
			long numeroContribuableMenage;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée le ménage
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noConjoint);
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noPrincipal);

				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, date(2000, 1, 1), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				numeros.numeroContribuableMenage = menage.getNumero();
				principal = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				numeros.numeroContribuablePrincipal = principal.getNumero();

				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2000, 1, 1), null);
				conjoint = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				numeros.numeroContribuableConjoint = conjoint.getNumero();

				// Crée le tuteur
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// Crée la tutelle proprement dites
				rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 1, 1));
				rapport.setDateFin(date(2007, 12, 31));
				rapport.setObjet(tuteur);
				rapport.setSujet(conjoint);
				tiersDAO.save(rapport);
				return null;
			}
		});
		// Vérification des adresses du principal
		{
			final Tiers principal = tiersService.getTiers(numeros.numeroContribuablePrincipal);
			assertNotNull(principal);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(principal, false);
			assertNotNull(adresses);
			assertEquals(1, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}

		// Vérification des adresses du conjoint
		{
			final Tiers conjoint = tiersService.getTiers(numeros.numeroContribuableConjoint);
			assertNotNull(conjoint);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(conjoint, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2003, 12, 31), "Bex", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2004, 1, 1), date(2007, 12, 31), "Cossonay-Ville", Source.TUTELLE, false, adresses.courrier.get(1));
			assertAdresse(date(2008, 1, 1), null, "Bex", Source.CIVILE, true, adresses.courrier.get(2));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}

		// Vérification des adresses du ménage
		{
			final Tiers menage = tiersService.getTiers(numeros.numeroContribuableMenage);
			assertNotNull(menage);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(menage, false);
			assertNotNull(adresses);

			assertEquals(1, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	/**
	 * Cas d'un couple avec le principal et le conjoint sous tutelle pendant une certaine période.
	 *
	 * <pre>
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 * Adresse civile principal:     | Lausanne
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 * Adresse civile conjoint:      | Bex
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 *                               ¦- 2000.01.01
	 *                               ¦
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 * Adresse tuteur principal:     | Cossonay-Ville
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 *                               ¦- 1985.04.01
	 *                               ¦
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 * Adresse tuteur secondaire:    | Les Clées
	 *                               +---------------------------------------------------------------------------------------------------------------------------
	 *                               ¦- 1985.04.01
	 *                               ¦
	 *                               ¦                            +---------------------------------------------------------+
	 * Tutelle du principal:         ¦                            | Tutelle                                                 |
	 *                               ¦                            +---------------------------------------------------------+
	 *                               ¦                            ¦- 2004.01.01                                 2007.12.31 -¦
	 *                               ¦                            ¦                                                         ¦
	 *                               ¦                            ¦                            +---------------------------------------------------------+
	 * Tutelle du conjoint:          ¦                            ¦                            | Tutelle                                                 |
	 *                               ¦                            ¦                            +---------------------------------------------------------+
	 *                               ¦                            ¦                            ¦- 2005.07.01                ¦                2009.12.31 -¦
	 *                               ¦                            ¦                            ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------------------+-------------------------------------
	 * Adresse principal résultante: | Lausanne                   | Cossonay-Ville                                          | Lausanne
	 *                               +----------------------------+----------------------------+----------------------------+-------------------------------------
	 *                               ¦- 2000.01.01    2003.12.31 -¦- 2004.01.01                ¦                2007.12.31 -¦- 2008.01.01                ¦
	 *                               ¦                            ¦                            ¦                            ¦                            ¦
	 *                               +---------------------------------------------------------+---------------------------------------------------------+-------
	 * Adresse conjoint résultante:  | Bex                                                     | Les Clées                                               |
	 *                               +---------------------------------------------------------+---------------------------------------------------------+-------
	 *                               ¦- 2000.01.01                ¦                2005.06.30 -¦- 2005.07.01                ¦                2009.12.31 -¦- 2010.01.01
	 *                               ¦                            ¦                            ¦                            ¦
	 *                               +----------------------------+----------------------------+----------------------------+------------------------------------
	 * Adresse ménage résultante:    | Lausanne                   ¦ Bex                        ¦ Cossonay-Ville             ¦ Lausanne
	 *                               +----------------------------+----------------------------+----------------------------+------------------------------------
	 *                               ¦- 2000.01.01    2003.12.31 -¦- 2004.01.01    2005.06.30 -¦- 2005.07.01    2007.12.31 -¦- 2008.01.01
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoCoupleTutellePrincipalEtConjoint() throws Exception {
		final long noPrincipal = 2;
		final long noConjoint = 3;
		final long noTuteurPrincipal = 5;
		final long noTuteurConjoint = 7;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// le principal
				MockIndividu paul = addIndividu(noPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);

				// le conjoint
				MockIndividu jeanne = addIndividu(noConjoint, date(1954, 11, 2), "Dupont", "Jeanne", false);
				addAdresse(jeanne, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);
				marieIndividus(paul, jeanne, date(2000, 1, 1));

				// le tuteur du principal
				MockIndividu jean = addIndividu(noTuteurPrincipal, date(1966, 4, 2), "Dupneu", "Jean", true);
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);

				// le tuteur du conjoint
				MockIndividu jacky = addIndividu(noTuteurConjoint, date(1967, 4, 2), "Dutronc", "Jacky", true);
				addAdresse(jacky, EnumTypeAdresse.PRINCIPALE, MockRue.LesClees.ChampDuRaffour, null,
						date(1985, 4, 1), null);
			}
		});

		final class Numeros {
			long numeroContribuablePrincipal;
			long numeroContribuableConjoint;
			long numeroContribuableMenage;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée le ménage
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noConjoint);
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noPrincipal);

				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, date(2000, 1, 1), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				numeros.numeroContribuableMenage = menage.getNumero();
				principal = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				numeros.numeroContribuablePrincipal = principal.getNumero();

				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2000, 1, 1), null);
				conjoint = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				numeros.numeroContribuableConjoint = conjoint.getNumero();

				// Crée la tutelle sur le principal
				PersonnePhysique tuteurPrincipal = new PersonnePhysique(true);
				tuteurPrincipal.setNumeroIndividu(noTuteurPrincipal);
				tuteurPrincipal = (PersonnePhysique) tiersDAO.save(tuteurPrincipal);

				rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 1, 1));
				rapport.setDateFin(date(2007, 12, 31));
				rapport.setObjet(tuteurPrincipal);
				rapport.setSujet(principal);
				tiersDAO.save(rapport);

				// Crée la tutelle sur le conjoint
				PersonnePhysique tuteurConjoint = new PersonnePhysique(true);
				tuteurConjoint.setNumeroIndividu(noTuteurConjoint);
				tuteurConjoint = (PersonnePhysique) tiersDAO.save(tuteurConjoint);

				rapport = new Tutelle();
				rapport.setDateDebut(date(2005, 7, 1));
				rapport.setDateFin(date(2009, 12, 31));
				rapport.setObjet(tuteurConjoint);
				rapport.setSujet(conjoint);
				tiersDAO.save(rapport);
				return null;
			}
		});

		// Vérification des adresses du principal
		{
			final Tiers principal = tiersService.getTiers(numeros.numeroContribuablePrincipal);
			assertNotNull(principal);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(principal, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2003, 12, 31), "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2004, 1, 1), date(2007, 12, 31), "Cossonay-Ville", Source.TUTELLE, false, adresses.courrier.get(1));
			assertAdresse(date(2008, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier.get(2));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}

		// Vérification des adresses du conjoint
		{
			final Tiers conjoint = tiersService.getTiers(numeros.numeroContribuableConjoint);
			assertNotNull(conjoint);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(conjoint, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2005, 6, 30), "Bex", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2005, 7, 1), date(2009, 12, 31), "Clées, Les", Source.TUTELLE, false, adresses.courrier.get(1));
			assertAdresse(date(2010, 1, 1), null, "Bex", Source.CIVILE, true, adresses.courrier.get(2));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Bex", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}

		// Vérification des adresses du ménage
		{
			final Tiers menage = tiersService.getTiers(numeros.numeroContribuableMenage);
			assertNotNull(menage);

			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(menage, false);
			assertNotNull(adresses);

			assertEquals(4, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2003, 12, 31), "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2004, 1, 1), date(2005, 6, 30), "Bex", Source.CONJOINT, false, adresses.courrier.get(1));
			assertAdresse(date(2005, 7, 1), date(2007, 12, 31), "Cossonay-Ville", Source.TUTELLE, false, adresses.courrier.get(2));
			assertAdresse(date(2008, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier.get(3));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	/**
	 * Cas d'une adresse "autre tiers" pointant du tuteur vers la pupille et générant ainsi une récursion infinie.
	 */
	@Test
	public void testGetAdressesFiscalesDetectionRecursionInfinie() throws Exception {
		final long noPupille = 2;
		final long noTuteur = 5;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// la pupille
				MockIndividu paul = addIndividu(noPupille, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(2000, 1, 1), null);

				// le tuteur
				MockIndividu jean = addIndividu(noTuteur, date(1966, 4, 2), "Dupneu", "Jean", true);

				// adresses courriers
				addAdresse(jean, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);

				// adresses principales/poursuite
				addAdresse(jean, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(1985, 4, 1), null);
			}
		});

		final long numeroContribuablePupille = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée le pupille et le tuteur
				PersonnePhysique pupille = new PersonnePhysique(true);
				pupille.setNumeroIndividu(noPupille);
				pupille = (PersonnePhysique) tiersDAO.save(pupille);

				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);

				// Crée l'adresse qui provoque une récursion infinie
				AdresseAutreTiers adresse = new AdresseAutreTiers();
				adresse.setDateDebut(date(2000, 1, 1));
				adresse.setDateFin(null);
				adresse.setUsage(TypeAdresseTiers.REPRESENTATION);
				adresse.setAutreTiersId(pupille.getId());
				adresse.setType(TypeAdresseTiers.COURRIER);
				tuteur.addAdresseTiers(adresse);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// Crée la tutelle proprement dites
				RapportEntreTiers rapport = new Tutelle();
				rapport.setDateDebut(date(2000, 1, 1));
				rapport.setDateFin(null);
				rapport.setObjet(tuteur);
				rapport.setSujet(pupille);
				tiersDAO.save(rapport);

				return pupille.getNumero();
			}
		});

		// Vérification de la détection du cycle
		{
			final Tiers pupille = tiersService.getTiers(numeroContribuablePupille);
			assertNotNull(pupille);

			try {
				adresseService.getAdressesFiscales(pupille, date(2000, 1, 1), false);
				fail();
			}
			catch (AdressesResolutionException ok) {
				// ok
			}

			try {
				adresseService.getAdresseFiscale(pupille, TypeAdresseFiscale.COURRIER, date(2000, 1, 1), false);
				fail();
			}
			catch (AdressesResolutionException ok) {
				// ok
			}

			try {
				adresseService.getAdressesFiscalHisto(pupille, false);
				fail();
			}
			catch (AdressesResolutionException ok) {
				// ok
			}
		}
	}

	/**
	 * Cas d'une adresse tiers "adresse civile" pointant vers l'adresse principale (et qui ne doit pas générer de récursion infinie).
	 *
	 * <pre>
	 *                              +-----------------------------------------------------------------------------
	 * Adresses civiles courrier:   | Lausanne
	 *                              +-----------------------------------------------------------------------------
	 *                              ¦- 2000.01.01
	 *                              ¦
	 *                              +-----------------------------------------------------------------------------
	 * Adresses civiles principale: | Cossonay-Ville
	 *                              +-----------------------------------------------------------------------------
	 *                              ¦- 2000.01.01
	 *                              ¦
	 *                              ¦                            +------------------------------------------------
	 * Adresses 'tiers' courrier:   ¦                            | (redirection vers adresse civile principale)
	 *                              ¦                            +------------------------------------------------
	 *                              ¦                            ¦- 2000.03.20
	 *                              ¦                            ¦
	 *                              +----------------------------+------------------------------------------------
	 * Adresses résultantes:        | Lausanne                   | Cossonay-Ville
	 *                              +----------------------------+------------------------------------------------
	 *                              ¦- 2000.01.01    2000.03.19 -¦- 2000.03.20
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoAvecAdresseCivile() throws Exception {

		final long noIndividu = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null,
						date(2000, 1, 1), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		{
			AdresseCivile adresse = new AdresseCivile();
			adresse.setDateDebut(date(2000, 3, 20));
			adresse.setDateFin(null);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setType(EnumTypeAdresse.PRINCIPALE);
			habitant.addAdresseTiers(adresse);
		}

		tiersDAO.save(habitant);

		{
			// Vérification des adresses
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
			assertNotNull(adresses);

			assertEquals(2, adresses.courrier.size());
			assertAdresse(date(2000, 1, 1), date(2000, 3, 19), "Lausanne", Source.CIVILE, false, adresses.courrier.get(0));
			assertAdresse(date(2000, 3, 20), null, "Cossonay-Ville", Source.FISCALE, false, adresses.courrier.get(1));

			assertEquals(1, adresses.representation.size());
			assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adresses.representation.get(0));

			assertEquals(1, adresses.poursuite.size());
			assertAdresse(date(2000, 1, 1), null, "Cossonay-Ville", Source.CIVILE, false, adresses.poursuite.get(0));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	/**
	 * Cas d'application des adresses par défaut au niveau du civile : adresses principales manquantes.
	 *
	 * <pre>
	 *                                +--------------------------+------------------------------+----------------------
	 * Adresses civiles courrier:     | Lausanne                 | Cossonay-Ville               | Les Clées
	 *                                +--------------------------+------------------------------+----------------------
	 *                                ¦- 1980.01.01  1987.12.11 -¦- 1988.12.12      2001.06.03 -¦- 2001.06.04
	 *                                ¦
	 *                                +--------------------------------------------------------------------------------
	 * Adresses civiles principale:   |
	 *                                +--------------------------------------------------------------------------------
	 *                                ¦
	 *                                ¦
	 *                                ¦
	 * Adresses résultantes:          ¦
	 *                                +--------------------------+------------------------------+----------------------
	 *  - courrier/représentation     | Lausanne (non-défaut)    | Cossonay-Ville (non-défaut)  | Les Clées (non-défaut)
	 *                                +--------------------------+------------------------------+----------------------
	 *                                ¦- 1980.01.01  1987.12.11 -¦- 1988.12.12      2001.06.03 -¦- 2001.06.04
	 *                                ¦
	 *                                +--------------------------+------------------------------+----------------------
	 *  - poursuite/domicile          | Lausanne (défaut)        | Cossonay-Ville (défaut)      | Les Clées (défaut)
	 *                                +--------------------------+------------------------------+----------------------
	 *                                ¦- 1980.01.01  1987.12.11 -¦- 1988.12.12      2001.06.03 -¦- 2001.06.04
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoDefaultAdressesPrincipalCivil() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1,
						1), date(1987, 12, 11));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						date(1987, 12, 12), date(2001, 6, 3));
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null,
						date(2001, 6, 4), null);
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		tiersDAO.save(habitant);

		// Vérification des adresses ponctuelles
		{
			{
				final AdressesFiscales adresses1950 = adresseService.getAdressesFiscales(habitant, date(1950, 1, 1), false);
				assertNotNull(adresses1950);
				assertNull(adresses1950.courrier);
				assertNull(adresses1950.representation);
				assertNull(adresses1950.poursuite);
				assertNull(adresses1950.domicile);

				assertAdressesByTypeEquals(adresses1950, habitant, date(1950, 1, 1));
			}

			{
				final AdressesFiscales adresses1982 = adresseService.getAdressesFiscales(habitant, date(1982, 1, 1), false);
				assertNotNull(adresses1982);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, false, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, false, adresses1982.representation);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, true, adresses1982.poursuite);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, true, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, habitant, date(1982, 1, 1));
			}

			{
				final AdressesFiscales adresses1995 = adresseService.getAdressesFiscales(habitant, date(1995, 1, 1), false);
				assertNotNull(adresses1995);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, false, adresses1995.courrier);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, false, adresses1995.representation);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, true, adresses1995.poursuite);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, true, adresses1995.domicile);

				assertAdressesByTypeEquals(adresses1995, habitant, date(1995, 1, 1));
			}

			{
				final AdressesFiscales adresses2004 = adresseService.getAdressesFiscales(habitant, date(2004, 1, 1), false);
				assertNotNull(adresses2004);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, false, adresses2004.courrier);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, false, adresses2004.representation);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, true, adresses2004.poursuite);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, true, adresses2004.domicile);

				assertAdressesByTypeEquals(adresses2004, habitant, date(2004, 1, 1));
			}
		}

		// Vérification des adresses historiques
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, false, adresses.courrier.get(0));
			assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, false, adresses.courrier.get(1));
			assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, false, adresses.courrier.get(2));

			assertAdressesEquals(adresses.courrier, adresses.representation);

			assertEquals(3, adresses.poursuite.size());
			assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, true, adresses.poursuite.get(0));
			assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, true, adresses.poursuite.get(1));
			assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, true, adresses.poursuite.get(2));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	/**
	 * Cas d'application des adresses par défaut au niveau du civile : adresses courrier manquantes.
	 *
	 * <pre>
	 *                                +--------------------------+------------------------------+----------------------
	 * Adresses civiles principale:   | Lausanne                 | Cossonay-Ville               | Les Clées
	 *                                +--------------------------+------------------------------+----------------------
	 *                                ¦- 1980.01.01  1987.12.11 -¦- 1988.12.12      2001.06.03 -¦- 2001.06.04
	 *                                ¦
	 *                                +--------------------------------------------------------------------------------
	 * Adresses civiles courrier:     |
	 *                                +--------------------------------------------------------------------------------
	 *                                ¦
	 *                                ¦
	 *                                ¦
	 * Adresses résultantes:          ¦
	 *                                +--------------------------+------------------------------+----------------------
	 *  - courrier/représentation     | Lausanne (défaut)        | Cossonay-Ville (défaut)      | Les Clées (défaut)
	 *                                +--------------------------+------------------------------+----------------------
	 *                                ¦- 1980.01.01  1987.12.11 -¦- 1988.12.12      2001.06.03 -¦- 2001.06.04
	 *                                ¦
	 *                                +--------------------------+------------------------------+----------------------
	 *  - poursuite/domicile          | Lausanne (non-défaut)    | Cossonay-Ville (non-défaut)  | Les Clées (non-défaut)
	 *                                +--------------------------+------------------------------+----------------------
	 *                                ¦- 1980.01.01  1987.12.11 -¦- 1988.12.12      2001.06.03 -¦- 2001.06.04
	 * </pre>
	 */
	@Test
	public void testGetAdressesFiscalesHistoDefaultAdressesCourrierCivil() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses principales
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980,
						1, 1), date(1987, 12, 11));
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						date(1987, 12, 12), date(2001, 6, 3));
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.LesClees.ChampDuRaffour, null, date(2001, 6,
						4), null);
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		tiersDAO.save(habitant);

		// Vérification des adresses ponctuelles
		{
			{
				final AdressesFiscales adresses1950 = adresseService.getAdressesFiscales(habitant, date(1950, 1, 1), false);
				assertNotNull(adresses1950);
				assertNull(adresses1950.courrier);
				assertNull(adresses1950.representation);
				assertNull(adresses1950.poursuite);
				assertNull(adresses1950.domicile);

				assertAdressesByTypeEquals(adresses1950, habitant, date(1950,1,1));
			}

			{
				final AdressesFiscales adresses1982 = adresseService.getAdressesFiscales(habitant, date(1982, 1, 1), false);
				assertNotNull(adresses1982);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, true, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, true, adresses1982.representation);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, false, adresses1982.poursuite);
				assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, false, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, habitant, date(1982, 1, 1));
			}

			{
				final AdressesFiscales adresses1995 = adresseService.getAdressesFiscales(habitant, date(1995, 1, 1), false);
				assertNotNull(adresses1995);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, true, adresses1995.courrier);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, true, adresses1995.representation);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, false, adresses1995.poursuite);
				assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, false, adresses1995.domicile);

				assertAdressesByTypeEquals(adresses1995, habitant, date(1995, 1, 1));
			}

			{
				final AdressesFiscales adresses2004 = adresseService.getAdressesFiscales(habitant, date(2004, 1, 1), false);
				assertNotNull(adresses2004);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, true, adresses2004.courrier);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, true, adresses2004.representation);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, false, adresses2004.poursuite);
				assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, false, adresses2004.domicile);

				assertAdressesByTypeEquals(adresses2004, habitant, date(2004, 1, 1));
			}
		}

		// Vérification des adresses historiques
		{
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, true, adresses.courrier.get(1));
			assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, true, adresses.courrier.get(2));

			assertAdressesEquals(adresses.courrier, adresses.representation);

			assertEquals(3, adresses.poursuite.size());
			assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.CIVILE, false, adresses.poursuite.get(0));
			assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.CIVILE, false, adresses.poursuite.get(1));
			assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.CIVILE, false, adresses.poursuite.get(2));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	@Test
	public void testGetAdressesFiscalesHistoEntrepriseSansAdresseFiscale() throws Exception {

		final long noEntreprise = 1;

		/*
		 * Crée les données du mock service PM
		 */
		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				MockPersonneMorale ent = addPM(noEntreprise, "Ma Petite Entreprise", "S.A.R.L.", date(1970, 7, 1), null);

				// adresses courriers
				addAdresse(ent, EnumTypeAdresseEntreprise.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, date(
						1980, 1, 1), date(1987, 12, 11));
				addAdresse(ent, EnumTypeAdresseEntreprise.COURRIER, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						MockLocalite.CossonayVille, date(1987, 12, 12), date(2001, 6, 3));
				addAdresse(ent, EnumTypeAdresseEntreprise.COURRIER, MockRue.LesClees.ChampDuRaffour, null, MockLocalite.LesClees, date(
						2001, 6, 4), null);

				// adresses principales/poursuite
				addAdresse(ent, EnumTypeAdresseEntreprise.SIEGE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, date(1980,
						1, 1), date(1987, 12, 11));
				addAdresse(ent, EnumTypeAdresseEntreprise.SIEGE, MockRue.CossonayVille.CheminDeRiondmorcel, null,
						MockLocalite.CossonayVille, date(1987, 12, 12), null);
			}
		});

		final long noCtbEntreprise = 12345;
		{
			// Crée une entreprise sans adresse fiscale surchargée
			Entreprise entreprise = new Entreprise();
			entreprise.setNumero(noCtbEntreprise);
			entreprise.setNumeroEntreprise(noEntreprise);
			tiersDAO.save(entreprise);
		}

		{
			final Entreprise entreprise = (Entreprise) tiersDAO.get(noCtbEntreprise);

			// Vérification des adresses
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(entreprise, false);
			assertNotNull(adresses);

			assertEquals(3, adresses.courrier.size());
			assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.PM, false, adresses.courrier.get(0));
			assertAdresse(date(1987, 12, 12), date(2001, 6, 3), "Cossonay-Ville", Source.PM, false, adresses.courrier.get(1));
			assertAdresse(date(2001, 6, 4), null, "Clées, Les", Source.PM, false, adresses.courrier.get(2));

			assertAdressesEquals(adresses.courrier, adresses.representation);

			assertEquals(2, adresses.poursuite.size());
			assertAdresse(date(1980, 1, 1), date(1987, 12, 11), "Lausanne", Source.PM, false, adresses.poursuite.get(0));
			assertAdresse(date(1987, 12, 12), null, "Cossonay-Ville", Source.PM, false, adresses.poursuite.get(1));

			assertAdressesEquals(adresses.poursuite, adresses.domicile);
		}
	}

	@Test
	public void testGetSalutations() {

		/* (création d'un adresse service à la main, pour pouver appeler une méthode protégée sans se heurter au proxy spring.) */
		AdresseServiceImpl service = new AdresseServiceImpl();
		service.setTiersService(tiersService);

		// Ménage homme-femme
		{
			PersonnePhysique arnold = new PersonnePhysique(false);
			arnold.setPrenom("Arnold");
			arnold.setNom("Schwarzie");
			arnold.setSexe(Sexe.MASCULIN);

			PersonnePhysique nolwen = new PersonnePhysique(false);
			nolwen.setPrenom("Nowlen");
			nolwen.setNom("Raflss");
			nolwen.setSexe(Sexe.FEMININ);

			EnsembleTiersCouple ensemble = new EnsembleTiersCouple(null, arnold, nolwen);
			assertEquals("Monsieur et Madame", service.getFormulePolitesse(ensemble, null).salutations());
		}

		// Ménage femme-femme
		{
			PersonnePhysique cora = new PersonnePhysique(false);
			cora.setPrenom("Cora");
			cora.setNom("Hildebrand");
			cora.setSexe(Sexe.FEMININ);

			PersonnePhysique nolwen = new PersonnePhysique(false);
			nolwen.setPrenom("Nowlen");
			nolwen.setNom("Raflss");
			nolwen.setSexe(Sexe.FEMININ);

			EnsembleTiersCouple ensemble = new EnsembleTiersCouple(null, cora, nolwen);
			assertEquals("Mesdames", service.getFormulePolitesse(ensemble, null).salutations());
		}

		// Ménage homme-homme
		{
			PersonnePhysique arnold = new PersonnePhysique(false);
			arnold.setPrenom("Arnold");
			arnold.setNom("Schwarzie");
			arnold.setSexe(Sexe.MASCULIN);

			PersonnePhysique roch = new PersonnePhysique(false);
			roch.setPrenom("Roch");
			roch.setNom("Wouazine");
			roch.setSexe(Sexe.MASCULIN);

			EnsembleTiersCouple ensemble = new EnsembleTiersCouple(null, arnold, roch);
			assertEquals("Messieurs", service.getFormulePolitesse(ensemble, null).salutations());
		}

		// Ménage homme-<sexe inconnu>
		{
			PersonnePhysique arnold = new PersonnePhysique(false);
			arnold.setPrenom("Arnold");
			arnold.setNom("Schwarzie");
			arnold.setSexe(Sexe.MASCULIN);

			PersonnePhysique alf = new PersonnePhysique(false);
			alf.setPrenom("Alf");
			alf.setNom("Alf");
			alf.setSexe(null);

			EnsembleTiersCouple ensemble = new EnsembleTiersCouple(null, arnold, alf);
			assertEquals("Madame, Monsieur", service.getFormulePolitesse(ensemble, null).salutations());
		}

		// Ménage femme-<sexe inconnu>
		{
			PersonnePhysique cora = new PersonnePhysique(false);
			cora.setPrenom("Cora");
			cora.setNom("Hildebrand");
			cora.setSexe(Sexe.FEMININ);

			PersonnePhysique alf = new PersonnePhysique(false);
			alf.setPrenom("Alf");
			alf.setNom("Alf");
			alf.setSexe(null);

			EnsembleTiersCouple ensemble = new EnsembleTiersCouple(null, cora, alf);
			assertEquals("Madame, Monsieur", service.getFormulePolitesse(ensemble, null).salutations());
		}

		// Ménage <sexe inconnu>-<sexe inconnu>
		{
			PersonnePhysique esc = new PersonnePhysique(false);
			esc.setPrenom("Escar");
			esc.setNom("Got");
			esc.setSexe(null);

			PersonnePhysique alf = new PersonnePhysique(false);
			alf.setPrenom("Alf");
			alf.setNom("Alf");
			alf.setSexe(null);

			EnsembleTiersCouple ensemble = new EnsembleTiersCouple(null, esc, alf);
			assertEquals("Madame, Monsieur", service.getFormulePolitesse(ensemble, null).salutations());
		}
	}

	/**
	 * Test du cas JIRA UNIREG-461
	 */
	@Test
	public void testAddAdresseSurNonHabitant() throws Exception {

		// Données d'entrées
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("Pauly");
		nh.setPrenom("Marco");
		nh.setDateNaissance(date(1970, 3, 2));

		AdresseSuisse courrier = new AdresseSuisse();
		courrier.setUsage(TypeAdresseTiers.COURRIER);
		courrier.setDateDebut(date(1988, 3, 2));
		courrier.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
		courrier.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());

		nh.addAdresseTiers(courrier);
		nh = (PersonnePhysique) tiersDAO.save(nh);

		// Vérification des adresses fiscales en l'état
		AdressesFiscales adresses = adresseService.getAdressesFiscales(nh, null, false);
		assertNotNull(adresses);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, false, adresses.courrier);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adresses.representation);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adresses.poursuite);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adresses.domicile);
		assertAdressesByTypeEquals(adresses, nh, null);

		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(nh, false);
		assertNotNull(adressesHisto);
		assertEquals(1, adressesHisto.courrier.size());
		assertEquals(1, adressesHisto.representation.size());
		assertEquals(1, adressesHisto.poursuite.size());
		assertEquals(1, adressesHisto.domicile.size());
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, false, adressesHisto.courrier.get(0));
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adressesHisto.representation.get(0));
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adressesHisto.poursuite.get(0));
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adressesHisto.domicile.get(0));

		// Ajout d'une adresse représentation
		AdresseSuisse representation = new AdresseSuisse();
		representation.setUsage(TypeAdresseTiers.REPRESENTATION);
		representation.setDateDebut(date(2008, 9, 18));
		representation.setNumeroRue(MockRue.Orbe.RueDavall.getNoRue());
		representation.setNumeroOrdrePoste(MockLocalite.Orbe.getNoOrdre());

		adresseService.addAdresse(nh, representation);

		// Vérification des adresses fiscales après ajout
		adresses = adresseService.getAdressesFiscales(nh, null, false);
		assertNotNull(adresses);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, false, adresses.courrier);
		assertAdresse(date(2008, 9, 18), null, "Orbe", Source.FISCALE, false, adresses.representation);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adresses.poursuite);
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adresses.domicile);
		assertAdressesByTypeEquals(adresses, nh, null);

		adressesHisto = adresseService.getAdressesFiscalHisto(nh, false);
		assertNotNull(adressesHisto);
		assertEquals(1, adressesHisto.courrier.size());
		assertEquals(2, adressesHisto.representation.size());
		assertEquals(1, adressesHisto.poursuite.size());
		assertEquals(1, adressesHisto.domicile.size());
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, false, adressesHisto.courrier.get(0));
		assertAdresse(date(1988, 3, 2), date(2008, 9, 17), "Lausanne", Source.FISCALE, true, adressesHisto.representation.get(0));
		assertAdresse(date(2008, 9, 18), null, "Orbe", Source.FISCALE, false, adressesHisto.representation.get(1));
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adressesHisto.poursuite.get(0));
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, true, adressesHisto.domicile.get(0));
	}

	@Test
	public void testAddAdresseSansAdresseExistante() throws Exception {

		// Données d'entrées
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("Pauly");
		nh.setPrenom("Marco");
		nh.setDateNaissance(date(1970, 3, 2));

		nh = (PersonnePhysique) tiersDAO.save(nh);

		// Ajoute d'une nouvelle adresse
		AdresseSuisse nouvelle = new AdresseSuisse();
		nouvelle.setUsage(TypeAdresseTiers.COURRIER);
		nouvelle.setDateDebut(date(2001, 1, 1));
		nouvelle.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		nouvelle.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
		adresseService.addAdresse(nh, nouvelle);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(nh, false);
		assertNotNull(adressesHisto);
		assertEquals(1, adressesHisto.courrier.size());
		assertAdresse(date(2001, 1, 1), null, "Bex", Source.FISCALE, false, adressesHisto.courrier.get(0));
	}

	@Test
	public void testAddAdresseAvecAdresseFiscaleExistante() throws Exception {

		// Données d'entrées
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("Pauly");
		nh.setPrenom("Marco");
		nh.setDateNaissance(date(1970, 3, 2));

		AdresseSuisse courrier = new AdresseSuisse();
		courrier.setUsage(TypeAdresseTiers.COURRIER);
		courrier.setDateDebut(date(1988, 3, 2));
		courrier.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
		courrier.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());

		nh.addAdresseTiers(courrier);
		nh = (PersonnePhysique) tiersDAO.save(nh);

		// Ajoute d'une nouvelle adresse
		AdresseSuisse nouvelle = new AdresseSuisse();
		nouvelle.setUsage(TypeAdresseTiers.COURRIER);
		nouvelle.setDateDebut(date(2001, 1, 1));
		nouvelle.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		nouvelle.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
		adresseService.addAdresse(nh, nouvelle);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(nh, false);
		assertNotNull(adressesHisto);
		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(1988, 3, 2), date(2000, 12, 31), "Lausanne", Source.FISCALE, false, adressesHisto.courrier.get(0));
		assertAdresse(date(2001, 1, 1), null, "Bex", Source.FISCALE, false, adressesHisto.courrier.get(1));
	}

	@Test
	public void testAddAdresseAvecAdresseCourrierExistante() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1,
						1), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980,
						1, 1), null);
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		// Ajoute d'une nouvelle adresse
		AdresseSuisse nouvelle = new AdresseSuisse();
		nouvelle.setUsage(TypeAdresseTiers.COURRIER);
		nouvelle.setDateDebut(date(2001, 1, 1));
		nouvelle.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		nouvelle.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
		adresseService.addAdresse(habitant, nouvelle);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adressesHisto);
		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(1980, 1, 1), date(2000, 12, 31), "Lausanne", Source.CIVILE, false, adressesHisto.courrier.get(0));
		assertAdresse(date(2001, 1, 1), null, "Bex", Source.FISCALE, false, adressesHisto.courrier.get(1));
	}

	@Test
	public void testAddAdresseAvecAdressesFiscaleEtCourrierExistantes() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1,
						1), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980,
						1, 1), null);
			}
		});

		// Crée un habitant avec une adresse fiscale surchargée
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);

		AdresseSuisse courrier = new AdresseSuisse();
		courrier.setUsage(TypeAdresseTiers.COURRIER);
		courrier.setDateDebut(date(1988, 3, 2));
		courrier.setDateFin(date(1995, 1, 1));
		courrier.setNumeroRue(MockRue.Orbe.RueDavall.getNoRue());
		courrier.setNumeroOrdrePoste(MockLocalite.Orbe.getNoOrdre());
		habitant.addAdresseTiers(courrier);

		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		// Ajoute d'une nouvelle adresse
		AdresseSuisse nouvelle = new AdresseSuisse();
		nouvelle.setUsage(TypeAdresseTiers.COURRIER);
		nouvelle.setDateDebut(date(2001, 1, 1));
		nouvelle.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		nouvelle.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
		adresseService.addAdresse(habitant, nouvelle);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(habitant, false);
		assertNotNull(adressesHisto);
		assertEquals(4, adressesHisto.courrier.size());
		assertAdresse(date(1980, 1, 1), date(1988, 3, 1), "Lausanne", Source.CIVILE, false, adressesHisto.courrier.get(0));
		assertAdresse(date(1988, 3, 2), date(1995, 1, 1), "Orbe", Source.FISCALE, false, adressesHisto.courrier.get(1));
		assertAdresse(date(1995, 1, 2), date(2000, 12, 31), "Lausanne", Source.CIVILE, false, adressesHisto.courrier.get(2));
		assertAdresse(date(2001, 1, 1), null, "Bex", Source.FISCALE, false, adressesHisto.courrier.get(3));
	}

	@Test
	public void testAnnulerAdresseSansAdressePrecedenteExistante() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Données d'entrées
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Pauly");
				nh.setPrenom("Marco");
				nh.setDateNaissance(date(1970, 3, 2));

				AdresseSuisse adresse = new AdresseSuisse();
				adresse.setUsage(TypeAdresseTiers.COURRIER);
				adresse.setDateDebut(date(2001, 1, 1));
				adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				nh.addAdresseTiers(adresse);

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers tiers = tiersDAO.get(id);
		AdresseTiers adresse = tiers.getAdressesTiersSorted().get(0);

		// Annulation de l'adresse
		adresseService.annulerAdresse(adresse);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiers, false);
		assertNotNull(adressesHisto);
		assertEquals(1, adressesHisto.courrier.size());

		final AdresseGenerique courrier0 = adressesHisto.courrier.get(0);
		assertTrue(courrier0.isAnnule());
	}

	@Test
	public void testAnnulerAdresseAvecAdresseFiscalePrecedenteExistante() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Données d'entrées
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Pauly");
				nh.setPrenom("Marco");
				nh.setDateNaissance(date(1970, 3, 2));

				AdresseSuisse adresse1 = new AdresseSuisse();
				adresse1.setUsage(TypeAdresseTiers.COURRIER);
				adresse1.setDateDebut(date(1988, 3, 2));
				adresse1.setDateFin(date(2000, 12, 31));
				adresse1.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
				adresse1.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
				nh.addAdresseTiers(adresse1);

				AdresseSuisse adresse2 = new AdresseSuisse();
				adresse2.setUsage(TypeAdresseTiers.COURRIER);
				adresse2.setDateDebut(date(2001, 1, 1));
				adresse2.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse2.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				nh.addAdresseTiers(adresse2);

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers tiers = tiersDAO.get(id);
		AdresseTiers adresse = tiers.getAdressesTiersSorted().get(1);

		// Annulation de l'adresse
		adresseService.annulerAdresse(adresse);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiers, false);
		assertNotNull(adressesHisto);
		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(1988, 3, 2), null, "Lausanne", Source.FISCALE, false, adressesHisto.courrier.get(0));
		assertTrue(adressesHisto.courrier.get(1).isAnnule());
	}

	/**
	 * [UNIREG-1580] Cas du contribuable n°107.147.00
	 */
	@Test
	public void testAnnulerAdresseAvecAdresseFiscalePrecedenteExistanteMaisAvecDateFinNulle() throws Exception {

		loadDatabase("classpath:ch/vd/uniregctb/adresse/TiersAvecDeuxAdressesFiscalesAvecDatesFinNulles.xml");
		
		serviceCivil.setUp(new DefaultMockServiceCivil(false));

		final long id =10714700L;
		final Tiers tiers = tiersDAO.get(id);

		final AdresseTiers adresse0 = tiers.getAdressesTiersSorted().get(0);
		assertFalse(adresse0.isAnnule());
		assertNull(adresse0.getDateFin());
		final AdresseTiers adresse1 = tiers.getAdressesTiersSorted().get(1);
		assertFalse(adresse1.isAnnule());
		assertNull(adresse1.getDateFin());

		// Annulation de l'adresse
		adresseService.annulerAdresse(adresse1);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiers, false);
		assertNotNull(adressesHisto);
		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(2009, 8, 29), null, "Genève Secteur de dist.", Source.FISCALE, false, adressesHisto.courrier.get(0));
		assertTrue(adressesHisto.courrier.get(1).isAnnule());
	}

	@Test
	public void testAnnulerAdresseAvecAdresseFiscalePrecedenteExistanteMaisNonAccolee() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Données d'entrées
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Pauly");
				nh.setPrenom("Marco");
				nh.setDateNaissance(date(1970, 3, 2));

				AdresseSuisse adresse1 = new AdresseSuisse();
				adresse1.setUsage(TypeAdresseTiers.COURRIER);
				adresse1.setDateDebut(date(1988, 3, 2));
				adresse1.setDateFin(date(2000, 12, 31));
				adresse1.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
				adresse1.setNumeroOrdrePoste(MockLocalite.Lausanne.getNoOrdre());
				nh.addAdresseTiers(adresse1);

				// du 2001.1.1 au 2003.12.31 -> pas d'adresse

				AdresseSuisse adresse2 = new AdresseSuisse();
				adresse2.setUsage(TypeAdresseTiers.COURRIER);
				adresse2.setDateDebut(date(2004, 1, 1));
				adresse2.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse2.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				nh.addAdresseTiers(adresse2);

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers tiers = tiersDAO.get(id);
		AdresseTiers adresse = tiers.getAdressesTiersSorted().get(1);

		// Annulation de l'adresse
		adresseService.annulerAdresse(adresse);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiers, false);
		assertNotNull(adressesHisto);
		assertEquals(2, adressesHisto.courrier.size());

		// adresses tiers non-accolées -> l'adresse précédente n'est *pas* réouverte
		assertAdresse(date(1988, 3, 2), date(2000, 12, 31), "Lausanne", Source.FISCALE, false, adressesHisto.courrier.get(0));
		assertTrue(adressesHisto.courrier.get(1).isAnnule());
	}

	@Test
	public void testAnnulerAdresseAvecAdresseCourrierPrecedenteExistante() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1,
						1), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980,
						1, 1), null);
			}
		});

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Données d'entrées
				// Crée un habitant sans adresse fiscale surchargée
				PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(noIndividu);
				habitant = (PersonnePhysique) tiersDAO.save(habitant);

				AdresseSuisse adresse = new AdresseSuisse();
				adresse.setUsage(TypeAdresseTiers.COURRIER);
				adresse.setDateDebut(date(2001, 1, 1));
				adresse.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				habitant.addAdresseTiers(adresse);

				habitant = (PersonnePhysique) tiersDAO.save(habitant);
				return habitant.getNumero();
			}
		});

		Tiers tiers = tiersDAO.get(id);
		AdresseTiers adresse = tiers.getAdressesTiersSorted().get(0);

		// Annulation de l'adresse
		adresseService.annulerAdresse(adresse);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiers, false);
		assertNotNull(adressesHisto);
		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.courrier.get(0));
		assertTrue(adressesHisto.courrier.get(1).isAnnule());
	}

	@Test
	public void testAnnulerAdresseAvecAdressesFiscaleEtCourrierExistantes() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1,
						1), null);

				// adresses principales/poursuite
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980,
						1, 1), null);
			}
		});

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Données d'entrées
				// Crée un habitant sans adresse fiscale surchargée
				PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(noIndividu);
				habitant = (PersonnePhysique) tiersDAO.save(habitant);

				AdresseSuisse adresse1 = new AdresseSuisse();
				adresse1.setUsage(TypeAdresseTiers.COURRIER);
				adresse1.setDateDebut(date(1988, 3, 2));
				adresse1.setDateFin(date(1995, 1, 1));
				adresse1.setNumeroRue(MockRue.Orbe.RueDavall.getNoRue());
				adresse1.setNumeroOrdrePoste(MockLocalite.Orbe.getNoOrdre());
				habitant.addAdresseTiers(adresse1);

				AdresseSuisse adresse2 = new AdresseSuisse();
				adresse2.setUsage(TypeAdresseTiers.COURRIER);
				adresse2.setDateDebut(date(2003, 3, 2));
				adresse2.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
				adresse2.setNumeroOrdrePoste(MockLocalite.Bex.getNoOrdre());
				habitant.addAdresseTiers(adresse2);

				habitant = (PersonnePhysique) tiersDAO.save(habitant);
				return habitant.getNumero();
			}
		});

		Tiers tiers = tiersDAO.get(id);
		AdresseTiers adresse = tiers.getAdressesTiersSorted().get(1);

		// Annulation de l'adresse
		adresseService.annulerAdresse(adresse);

		// Teste des adresses résultantes
		AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiers, false);
		assertNotNull(adressesHisto);
		assertEquals(4, adressesHisto.courrier.size());
		assertAdresse(date(1980, 1, 1), date(1988, 3, 1), "Lausanne", Source.CIVILE, false, adressesHisto.courrier.get(0));
		assertAdresse(date(1988, 3, 2), date(1995, 1, 1), "Orbe", Source.FISCALE, false, adressesHisto.courrier.get(1));
		assertAdresse(date(1995, 1, 2), null, "Lausanne", Source.CIVILE, false, adressesHisto.courrier.get(2));
		assertTrue(adressesHisto.courrier.get(3).isAnnule());
	}

	@Test
	public void testGetAdressesFiscalesMenageCommunAvecRapportsAnnules() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);

				// adresses courriers
				addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(2000, 1, 1), null);

				// adresses principales/poursuite
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1,
						1), null);

				MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);

				// adresses courriers
				addAdresse(virginie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002,
						2, 2), null);

				// adresses principales/poursuite
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002,
						2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));
			}
		});

		// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
		final long noMenageCommun = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, date(2004, 7, 14), null);
				rapport.setAnnule(true);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2004, 7, 14), null);
				rapport.setAnnule(true);
				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);
		assertEmpty(adressesHisto.courrier);
		assertEmpty(adressesHisto.domicile);
		assertEmpty(adressesHisto.poursuite);
		assertEmpty(adressesHisto.representation);

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, date(2008,1,1), false);
		assertNotNull(adresses);
		assertNull(adresses.courrier);
		assertNull(adresses.domicile);
		assertNull(adresses.poursuite);
		assertNull(adresses.representation);

		final List<String> nomCourrier = adresseService.getNomCourrier(menage, date(2005,1,1), false);
		assertEmpty(nomCourrier);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, date(2007,3,3), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertNull(adresseEnvoi.getLigne1());
		assertNull(adresseEnvoi.getLigne2());
		assertNull(adresseEnvoi.getLigne3());
		assertNull(adresseEnvoi.getLigne4());
		assertNull(adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun avec représentant est bien celle du représentant. 
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecRepresentant() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuRepresentant = 11;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuRepresentant, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique representant = addHabitant(noIndividuRepresentant);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2004, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				addRepresentationConventionnelle(menage, representant, date(2007, 1, 1), false);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), date(2006, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2007, 1, 1), null, "Le Sentier", Source.REPRESENTATION, false, adressesHisto.courrier.get(1));

		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdressesEquals(adressesHisto.courrier.get(1), adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun dont le composant principal possède un représentant n'est *pas* celle du représentant de ce dernier.
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecRepresentantSurPrincipal() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuRepresentant = 11;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuRepresentant, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant sur l'habitant principal
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique representant = addHabitant(noIndividuRepresentant);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2004, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				addRepresentationConventionnelle(principal, representant, date(2007, 1, 1), false);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(1, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdressesEquals(adressesHisto.courrier.get(0), adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun dont le conjoint possède un représentant reste bien celle du ménage (le représentant du conjoint est ignoré).
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecRepresentantSurConjoint() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuRepresentant = 11;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuRepresentant, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant sur l'habitant principal
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique representant = addHabitant(noIndividuRepresentant);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2004, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				addRepresentationConventionnelle(conjoint, representant, date(2007, 1, 1), false);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(1, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdressesEquals(adressesHisto.courrier.get(0), adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun avec un représentant sur lui-même et un autre représentant sur le principal est bien celle du représentant du ménage (le
	 * représentant du principal est ignoré).
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecRepresentantSurMenageEtSurPrincipal() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuRepresentantMenage = 11;
		final long noIndividuRepresentantPrincipal = 12;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuRepresentantMenage, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);

				final MockIndividu wendy = addIndividu(noIndividuRepresentantPrincipal, date(1945, 3, 17), "Wendy", "Lafrite", false);
				addAdresse(wendy, EnumTypeAdresse.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant sur l'habitant principal et un autre sur le ménage
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique representantMenage = addHabitant(noIndividuRepresentantMenage);
				PersonnePhysique representantPrincipal = addHabitant(noIndividuRepresentantPrincipal);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2004, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				addRepresentationConventionnelle(menage, representantMenage, date(2007, 1, 1), false);
				addRepresentationConventionnelle(principal, representantPrincipal, date(2005, 1, 1), false);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), date(2006, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2007, 1, 1), null, "Le Sentier", Source.REPRESENTATION, false, adressesHisto.courrier.get(1)); // représentant du ménage

		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdressesEquals(adressesHisto.courrier.get(1), adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun dont le conjoint possède un représentant et dont le principal est sous tutelle est l'adresse courrier du conjoint (et pas celle du
	 * son représentant).
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecRepresentantSurConjointEtPrincipalSousTutelle() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuRepresentant = 11;
		final long noIndividuTuteur = 12;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuRepresentant, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);

				final MockIndividu julien = addIndividu(noIndividuTuteur, date(1945, 3, 17), "Barouffe", "Julien", false);
				addAdresse(julien, EnumTypeAdresse.PRINCIPALE, MockRue.Lonay.CheminDuRechoz, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant sur l'habitant principal
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique representant = addHabitant(noIndividuRepresentant);
				PersonnePhysique tuteur = addHabitant(noIndividuTuteur);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2004, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				addRepresentationConventionnelle(conjoint, representant, date(2007, 1, 1), false);
				addTutelle(principal, tuteur, null, date(2007, 1, 1), null);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), date(2006, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2007, 1, 1), null, "Lausanne", Source.CONJOINT, false, adressesHisto.courrier.get(1)); // adresse du courrier du conjoint
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdressesEquals(adressesHisto.courrier.get(1), adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("Av de Marcelin", adresseEnvoi.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun avec un conseiller légal est bien celle du conseiller.
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecConseillerLegal() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuConseiller = 11;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuConseiller, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique conseiller = addHabitant(noIndividuConseiller);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2004, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				addConseilLegal(menage, conseiller, date(2007, 1, 1));

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(2, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), date(2006, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2007, 1, 1), null, "Le Sentier", Source.CONSEIL_LEGAL, false, adressesHisto.courrier.get(1));

		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdressesEquals(adressesHisto.courrier.get(1), adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun dans le cas où un des membres (ou les deux) est sous curatelle.
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecCurateurs() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuCurateurPrincipal = 11;
		final long noIndividuCurateurConjoint = 12;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuCurateurPrincipal, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);

				final MockIndividu julien = addIndividu(noIndividuCurateurConjoint, date(1945, 3, 17), "Barouffe", "Julien", false);
				addAdresse(julien, EnumTypeAdresse.PRINCIPALE, MockRue.Lonay.CheminDuRechoz, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique curateurPrincipal = addHabitant(noIndividuCurateurPrincipal);
				PersonnePhysique curateurConjoint = addHabitant(noIndividuCurateurConjoint);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2000, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				// 2003 : principal sous curatelle
				// 2004 : principal+conjoint sous curatelle
				// 2005 : conjoint sous curatelle
				addCuratelle(principal, curateurPrincipal, date(2003, 1, 1), date(2004, 12, 31));
				addCuratelle(conjoint, curateurConjoint, date(2004, 1, 1), date(2005, 12, 31));

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(4, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), date(2002, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2003, 1, 1), date(2003, 12, 31), "Bussigny-près-Lausanne", Source.CONJOINT, false, adressesHisto.courrier.get(1));
		assertAdresse(date(2004, 1, 1), date(2004, 12, 31), "Le Sentier", Source.CURATELLE, false, adressesHisto.courrier.get(2));
		assertAdresse(date(2005, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(3));

		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdresse(date(2005, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		// avant la curatelle
		final AdresseEnvoiDetaillee adresseEnvoi2002 = adresseService.getAdresseEnvoi(menage, date(2002, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2002);
		assertEquals("Monsieur et Madame", adresseEnvoi2002.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2002.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2002.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi2002.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi2002.getLigne5());
		assertNull(adresseEnvoi2002.getLigne6());

		// curatelle sur le principal
		final AdresseEnvoiDetaillee adresseEnvoi2003 = adresseService.getAdresseEnvoi(menage, date(2003, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2003);
		assertEquals("Monsieur et Madame", adresseEnvoi2003.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2003.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2003.getLigne3());
		assertEquals("Rue de l'Industrie", adresseEnvoi2003.getLigne4());
		assertEquals("1030 Bussigny-près-Lausanne", adresseEnvoi2003.getLigne5());
		assertNull(adresseEnvoi2003.getLigne6());

		// curatelle sur le principal et le conjoint
		final AdresseEnvoiDetaillee adresseEnvoi2004 = adresseService.getAdresseEnvoi(menage, date(2004, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2004);
		assertEquals("Monsieur et Madame", adresseEnvoi2004.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2004.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2004.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi2004.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi2004.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi2004.getLigne6());

		// curatelle sur le conjoint
		final AdresseEnvoiDetaillee adresseEnvoi2005 = adresseService.getAdresseEnvoi(menage, date(2005, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2005);
		assertEquals("Monsieur et Madame", adresseEnvoi2005.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2005.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2005.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi2005.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi2005.getLigne5());
		assertNull(adresseEnvoi2005.getLigne6());

		// plus du curatelle
		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
	}
	
	/**
	 * [UNIREG-1341] Vérifie que l'adresse courrier d'un ménage-commun dans le cas où un des membres (ou les deux) est sous tutelle.
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecTuteurs() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuTuteurPrincipal = 11;
		final long noIndividuTuteurConjoint = 12;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2002, 2, 2), null);

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuTuteurPrincipal, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);

				final MockIndividu julien = addIndividu(noIndividuTuteurConjoint, date(1945, 3, 17), "Barouffe", "Julien", false);
				addAdresse(julien, EnumTypeAdresse.PRINCIPALE, MockRue.Lonay.CheminDuRechoz, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants avec un représentant
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique curateurPrincipal = addHabitant(noIndividuTuteurPrincipal);
				PersonnePhysique curateurConjoint = addHabitant(noIndividuTuteurConjoint);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2000, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				// 2003 : principal sous tutelle
				// 2004 : principal+conjoint sous tutelle
				// 2005 : conjoint sous tutelle
				addTutelle(principal, curateurPrincipal, null, date(2003, 1, 1), date(2004, 12, 31));
				addTutelle(conjoint, curateurConjoint, null, date(2004, 1, 1), date(2005, 12, 31));

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(4, adressesHisto.courrier.size());
		assertAdresse(date(2000, 1, 1), date(2002, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2003, 1, 1), date(2003, 12, 31), "Bussigny-près-Lausanne", Source.CONJOINT, false, adressesHisto.courrier.get(1));
		assertAdresse(date(2004, 1, 1), date(2004, 12, 31), "Le Sentier", Source.TUTELLE, false, adressesHisto.courrier.get(2));
		assertAdresse(date(2005, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(3));

		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(menage, null, false);
		assertNotNull(adresses);
		assertAdresse(date(2005, 1, 1), null, "Lausanne", Source.CIVILE, true, adresses.courrier);
		assertAdressesEquals(adressesHisto.domicile.get(0), adresses.domicile);
		assertAdressesEquals(adressesHisto.poursuite.get(0), adresses.poursuite);
		assertAdressesEquals(adressesHisto.representation.get(0), adresses.representation);

		// avant la tutelle
		final AdresseEnvoiDetaillee adresseEnvoi2002 = adresseService.getAdresseEnvoi(menage, date(2002, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2002);
		assertEquals("Monsieur et Madame", adresseEnvoi2002.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2002.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2002.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi2002.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi2002.getLigne5());
		assertNull(adresseEnvoi2002.getLigne6());

		// tutelle sur le principal
		final AdresseEnvoiDetaillee adresseEnvoi2003 = adresseService.getAdresseEnvoi(menage, date(2003, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2003);
		assertEquals("Monsieur et Madame", adresseEnvoi2003.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2003.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2003.getLigne3());
		assertEquals("Rue de l'Industrie", adresseEnvoi2003.getLigne4());
		assertEquals("1030 Bussigny-près-Lausanne", adresseEnvoi2003.getLigne5());
		assertNull(adresseEnvoi2003.getLigne6());

		// tutelle sur le principal et le conjoint
		final AdresseEnvoiDetaillee adresseEnvoi2004 = adresseService.getAdresseEnvoi(menage, date(2004, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2004);
		assertEquals("Monsieur et Madame", adresseEnvoi2004.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2004.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2004.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi2004.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi2004.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi2004.getLigne6());

		// tutelle sur le conjoint
		final AdresseEnvoiDetaillee adresseEnvoi2005 = adresseService.getAdresseEnvoi(menage, date(2005, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2005);
		assertEquals("Monsieur et Madame", adresseEnvoi2005.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2005.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2005.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi2005.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi2005.getLigne5());
		assertNull(adresseEnvoi2005.getLigne6());

		// plus du tutelle
		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertEquals("Monsieur et Madame", adresseEnvoi.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
	}

	/**
	 * [UNIREG-2676] Vérifie que l'adresse courrier d'un ménage-commun dans le cas où le principal est sous-tutelle et le conjoint hors-Suisse est bien celle du tuteur du principal.
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecPrincipalSousTutelleEtConjointHorsSuisse() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuTuteurPrincipal = 11;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1953, 11, 2), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(1957, 1, 23), date(2004, 12, 31));
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, date(2005, 1, 1), date(2008, 12, 31));
				virginie.getAdresses().add(newAdresse(EnumTypeAdresse.COURRIER, "5 Avenue des Champs-Elysées", null, "75017 Paris", MockPays.France, date(2009, 1, 1), null));

				marieIndividus(paul, virginie, date(2004, 7, 14));

				final MockIndividu ronald = addIndividu(noIndividuTuteurPrincipal, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants dont le principal est sous tutelle
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique tuteurPrincipal = addHabitant(noIndividuTuteurPrincipal);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2000, 7, 14), null);
				MenageCommun menage = ensemble.getMenage();

				// 2000-2004 : principal sous tutelle, conjoint en Suisse
				// 2005-2008 : principal sous tutelle, conjoint hors canton
				// dès 2009 : principal sous tutelle, conjoint hors Suisse
				addTutelle(principal, tuteurPrincipal, null, date(2000, 1, 1), null);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(4, adressesHisto.courrier.size());
		assertAdresse(date(1953, 11, 2), date(1999, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0));
		assertAdresse(date(2000, 1, 1), date(2004, 12, 31), "Bussigny-près-Lausanne", Source.CONJOINT, false, adressesHisto.courrier.get(1));
		assertAdresse(date(2005, 1, 1), date(2008, 12, 31), "Neuchâtel", Source.CONJOINT, false, adressesHisto.courrier.get(2));
		assertAdresse(date(2009, 1, 1), null, "Le Sentier", Source.TUTELLE, false, adressesHisto.courrier.get(3));

		assertAdresse(date(1953, 11, 2), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(1953, 11, 2), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(1953, 11, 2), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		// conjoint en Suisse => adresse courrier du conjoint utilisée
		final AdresseEnvoiDetaillee adresseEnvoi2002 = adresseService.getAdresseEnvoi(menage, date(2002, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2002);
		assertEquals("Monsieur et Madame", adresseEnvoi2002.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2002.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2002.getLigne3());
		assertEquals("Rue de l'Industrie", adresseEnvoi2002.getLigne4());
		assertEquals("1030 Bussigny-près-Lausanne", adresseEnvoi2002.getLigne5());
		assertNull(adresseEnvoi2002.getLigne6());

		// conjoint hors canton => adresse courrier du conjoint utilisée
		final AdresseEnvoiDetaillee adresseEnvoi2006 = adresseService.getAdresseEnvoi(menage, date(2006, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2006);
		assertEquals("Monsieur et Madame", adresseEnvoi2006.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2006.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2006.getLigne3());
		assertEquals("Rue des Beaux-Arts", adresseEnvoi2006.getLigne4());
		assertEquals("2000 Neuchâtel", adresseEnvoi2006.getLigne5());
		assertNull(adresseEnvoi2006.getLigne6());

		// conjoint hors-Suisse => adresse courrier du conjoint ignorée
		final AdresseEnvoiDetaillee adresseEnvoi2010 = adresseService.getAdresseEnvoi(menage, date(2010, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2010);
		assertEquals("Monsieur et Madame", adresseEnvoi2010.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2010.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2010.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi2010.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi2010.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi2010.getLigne6());
	}

	/**
	 * [UNIREG-2644] Vérifie que l'adresse courrier d'un ménage-commun dans le cas où le principal est sous-tutelle et son conjoint décédé est bien celle du tuteur du principal.
	 */
	@Test
	public void testGetAdressesFiscalesMenageCommunAvecPrincipalSousTutelleEtConjointDecede() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noIndividuTuteurPrincipal = 11;

		final RegDate dateMariage = date(1980, 7, 14);
		final RegDate dateDeces = date(2009, 6, 12);

		// Un couple dont monsieur est sous tutelle et madame décédée
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu paul = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Dupont", "Paul", true);
				addAdresse(paul, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1953, 11, 2), null);

				final MockIndividu virginie = addIndividu(noIndividuConjoint, date(1957, 1, 23), "Dupont", "Virginie", false);
				addAdresse(virginie, EnumTypeAdresse.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(1957, 1, 23), null);
				virginie.setDateDeces(dateDeces);

				marieIndividus(paul, virginie, dateMariage);
				addEtatCivil(paul, dateDeces, EnumTypeEtatCivil.VEUF);

				final MockIndividu ronald = addIndividu(noIndividuTuteurPrincipal, date(1945, 3, 17), "MacDonald", "Ronald", false);
				addAdresse(ronald, EnumTypeAdresse.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(1945, 3, 17), null);
			}
		});

		// Crée un ménage composé de deux habitants dont le principal est sous tutelle et madame décédée
		final long noMenageCommun = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique tuteurPrincipal = addHabitant(noIndividuTuteurPrincipal);

				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, dateMariage, dateDeces);
				MenageCommun menage = ensemble.getMenage();

				// 2000 : mise sous tutelle de monsieur
				// 2009 : décès de madame
				addTutelle(principal, tuteurPrincipal, null, date(2000, 1, 1), null);

				return menage.getNumero();
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);

		// Vérification des adresses
		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(menage, false);
		assertNotNull(adressesHisto);

		assertEquals(3, adressesHisto.courrier.size());
		assertAdresse(date(1953, 11, 2), date(1999, 12, 31), "Lausanne", Source.CIVILE, true, adressesHisto.courrier.get(0)); // adresse de Monsieur
		assertAdresse(date(2000, 1, 1), dateDeces.getOneDayBefore(), "Bussigny-près-Lausanne", Source.CONJOINT, false, adressesHisto.courrier.get(1)); // adresse de Madame
		assertAdresse(dateDeces, null, "Le Sentier", Source.TUTELLE, false, adressesHisto.courrier.get(2)); // adresse du tuteur de Monsieur

		assertAdresse(date(1953, 11, 2), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(0));
		assertAdresse(date(1953, 11, 2), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(0));
		assertAdresse(date(1953, 11, 2), null, "Lausanne", Source.CIVILE, true, adressesHisto.representation.get(0));

		// monsieur sans tutelle => adresse de monsieur
		final AdresseEnvoiDetaillee adresseEnvoi1990 = adresseService.getAdresseEnvoi(menage, date(1990, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi1990);
		assertEquals("Monsieur et Madame", adresseEnvoi1990.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi1990.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi1990.getLigne3());
		assertEquals("Av de Beaulieu", adresseEnvoi1990.getLigne4());
		assertEquals("1000 Lausanne", adresseEnvoi1990.getLigne5());
		assertNull(adresseEnvoi1990.getLigne6());

		// monsieur sous tutelle => adresse de madame
		final AdresseEnvoiDetaillee adresseEnvoi2002 = adresseService.getAdresseEnvoi(menage, date(2002, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2002);
		assertEquals("Monsieur et Madame", adresseEnvoi2002.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2002.getLigne2());
		assertEquals("Virginie Dupont", adresseEnvoi2002.getLigne3());
		assertEquals("Rue de l'Industrie", adresseEnvoi2002.getLigne4());
		assertEquals("1030 Bussigny-près-Lausanne", adresseEnvoi2002.getLigne5());
		assertNull(adresseEnvoi2002.getLigne6());

		// madame décédée et monsieur sous tutelle => adresse du tuteur de monsieur
		final AdresseEnvoiDetaillee adresseEnvoi2010 = adresseService.getAdresseEnvoi(menage, date(2010, 1, 1), TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresseEnvoi2010);
		assertEquals("Aux héritiers de", adresseEnvoi2010.getLigne1());
		assertEquals("Paul Dupont", adresseEnvoi2010.getLigne2());
		assertEquals("Virginie Dupont, défunte", adresseEnvoi2010.getLigne3());
		assertEquals("p.a. Ronald MacDonald", adresseEnvoi2010.getLigne4());
		assertEquals("Grande-Rue", adresseEnvoi2010.getLigne5());
		assertEquals("1347 Le Sentier", adresseEnvoi2010.getLigne6());
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableCelibataire() throws Exception {

		final long noTiers = 44018108;
		final long noIndividu = 381865;

		// un individu célibataire avec une adresse de domicile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1953, 11, 2), "Galley", "Philippe", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.GrangesMarnand.SousLeBois, null, date(2000, 1, 1), null);
			}
		});
		final PersonnePhysique ctb = addHabitant(noTiers, noIndividu);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, true, adresses.courrier);
		assertAdressesEquals(adresses.domicile, adresses.poursuite);
		assertAdressesEquals(adresses.courrier, adresses.representation);
		assertNull(adresses.poursuiteAutreTiers);

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Monsieur", domicile.getLigne1());
		assertEquals("Philippe Galley", domicile.getLigne2());
		assertEquals("Chemin Sous le Bois", domicile.getLigne3());
		assertEquals("1523 Granges-près-Marnand", domicile.getLigne4());
		assertNull(domicile.getLigne5());

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false));
		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false));
		assertNull(adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false));
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableCouple() throws Exception {

		final long noTiersPrincipal = 77619511;
		final long noTiersConjoint = 46713404;

		final long noIndividuPrincipal = 412949;
		final long noIndividuConjoint = 125125;

		// un individu célibataire avec une adresse de domicile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu principal = addIndividu(noIndividuPrincipal, date(1953, 11, 2), "Duc", "Jean-Jacques", true);
				addAdresse(principal, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), null);

				MockIndividu conjoint = addIndividu(noIndividuConjoint, date(1953, 11, 2), "Duc", "Regina", false);
				addAdresse(conjoint, EnumTypeAdresse.PRINCIPALE, MockRue.GrangesMarnand.RueDeVerdairuz, null, date(2000, 1, 1), null);
			}
		});
		final PersonnePhysique principal = addHabitant(noTiersPrincipal, noIndividuPrincipal);
		final PersonnePhysique conjoint = addHabitant(noTiersConjoint, noIndividuConjoint);
		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(2000, 1, 1), null);
		assertNotNull(ensemble);

		// les adresses fiscales
		final AdressesFiscales adressesPrincipal = adresseService.getAdressesFiscales(principal, null, false);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, false, adressesPrincipal.domicile);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CIVILE, true, adressesPrincipal.courrier);
		assertAdressesEquals(adressesPrincipal.domicile, adressesPrincipal.poursuite);
		assertAdressesEquals(adressesPrincipal.courrier, adressesPrincipal.representation);
		assertNull(adressesPrincipal.poursuiteAutreTiers);

		final AdressesFiscales adressesConjoint = adresseService.getAdressesFiscales(conjoint, null, false);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, false, adressesConjoint.domicile);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, true, adressesConjoint.courrier);
		assertAdressesEquals(adressesConjoint.domicile, adressesConjoint.poursuite);
		assertAdressesEquals(adressesConjoint.courrier, adressesConjoint.representation);
		assertNull(adressesConjoint.poursuiteAutreTiers);

		// les adresses d'envoi
		final AdresseEnvoi domicilePrincipal = adresseService.getAdresseEnvoi(principal, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Monsieur", domicilePrincipal.getLigne1());
		assertEquals("Jean-Jacques Duc", domicilePrincipal.getLigne2());
		assertEquals("Boulevard de Grancy", domicilePrincipal.getLigne3());
		assertEquals("1000 Lausanne", domicilePrincipal.getLigne4());
		assertNull(domicilePrincipal.getLigne5());

		assertEquals(domicilePrincipal, adresseService.getAdresseEnvoi(principal, null, TypeAdresseFiscale.COURRIER, false));
		assertEquals(domicilePrincipal, adresseService.getAdresseEnvoi(principal, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertEquals(domicilePrincipal, adresseService.getAdresseEnvoi(principal, null, TypeAdresseFiscale.POURSUITE, false));
		assertNull(adresseService.getAdresseEnvoi(principal, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false));

		final AdresseEnvoi domicileConjoint = adresseService.getAdresseEnvoi(conjoint, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Madame", domicileConjoint.getLigne1());
		assertEquals("Regina Duc", domicileConjoint.getLigne2());
		assertEquals("Rue de Verdairuz", domicileConjoint.getLigne3());
		assertEquals("1523 Granges-près-Marnand", domicileConjoint.getLigne4());
		assertNull(domicileConjoint.getLigne5());

		assertEquals(domicileConjoint, adresseService.getAdresseEnvoi(conjoint, null, TypeAdresseFiscale.COURRIER, false));
		assertEquals(domicileConjoint, adresseService.getAdresseEnvoi(conjoint, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertEquals(domicileConjoint, adresseService.getAdresseEnvoi(conjoint, null, TypeAdresseFiscale.POURSUITE, false));
		assertNull(adresseService.getAdresseEnvoi(conjoint, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false));
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableSousCuratelle() throws Exception {

		final long noTiers = 89016804;
		final long noCurateur = 13110204;

		final long noIndividu = 410156;
		final long noIndCurateur = 431638;

		// un contribuable sous curatelle avec un curateur possèdant une adresse de représentation sur Lausanne
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1953, 11, 2), "Staheli", "Marc", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2000, 1, 1), null);

				MockIndividu curateur = addIndividu(noIndCurateur, date(1953, 11, 2), "Bally", "Alain", true);
				addAdresse(curateur, EnumTypeAdresse.PRINCIPALE, MockRue.Vevey.RueDesMoulins, null, date(2000, 1, 1), null);
			}
		});
		final PersonnePhysique ctb = addHabitant(noTiers, noIndividu);
		final PersonnePhysique curateur = addHabitant(noCurateur, noIndCurateur);
		addAdresseSuisse(curateur, TypeAdresseTiers.REPRESENTATION, date(2000,1,1), null, MockRue.Lausanne.PlaceSaintFrancois);
		addCuratelle(ctb, curateur, date(2000, 1, 1), null);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Bussigny-près-Lausanne", Source.CIVILE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CURATELLE, false, adresses.courrier);
		assertAdressesEquals(adresses.domicile, adresses.poursuite);
		assertAdresse(date(2000, 1, 1), null, "Bussigny-près-Lausanne", Source.CIVILE, true, adresses.representation);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.CURATELLE, false, adresses.poursuiteAutreTiers);

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Monsieur", domicile.getLigne1());
		assertEquals("Marc Staheli", domicile.getLigne2());
		assertEquals("Rue de l'Industrie", domicile.getLigne3());
		assertEquals("1030 Bussigny-près-Lausanne", domicile.getLigne4());
		assertNull(domicile.getLigne5());

		final AdresseEnvoi courrier = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
		assertEquals("Monsieur", courrier.getLigne1());
		assertEquals("Marc Staheli", domicile.getLigne2());
		assertEquals("p.a. Alain Bally", courrier.getLigne3());
		assertEquals("Place Saint-François", courrier.getLigne4());
		assertEquals("1000 Lausanne", courrier.getLigne5());
		assertNull(courrier.getLigne6());

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false));

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false));

		final AdresseEnvoiDetaillee poursuiteAutreTiers = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false);
		assertEquals("Monsieur", poursuiteAutreTiers.getLigne1());
		assertEquals("Alain Bally", poursuiteAutreTiers.getLigne2());
		assertEquals("Place Saint-François", poursuiteAutreTiers.getLigne3());
		assertEquals("1000 Lausanne", poursuiteAutreTiers.getLigne4());
		assertNull(poursuiteAutreTiers.getLigne5());
		assertEquals(Source.CURATELLE, poursuiteAutreTiers.getSource());
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableSousTutelleSansAutoriteTutelaire() throws Exception {

		final long noTiers = 60510843;
		final long noIndividu = 750946;

		// un contribuable sous tutelle avec l'OTG (Lausanne) comme tuteur et sans autorité tutelaire renseignée
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1953, 11, 2), "Lopes", "Anabela", false);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), null);
			}
		});

		final PersonnePhysique ctb = addHabitant(noTiers, noIndividu);
		final CollectiviteAdministrative tuteur = addCollAdm(ServiceInfrastructureService.noTuteurGeneral);
		addTutelle(ctb, tuteur, null, date(2000, 1, 1), null);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Echallens", Source.CIVILE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.TUTELLE, false, adresses.courrier); // adresse du tuteur
		assertAdressesEquals(adresses.domicile, adresses.poursuite); // adresse de domicile de la pupille
		assertAdresse(date(2000, 1, 1), null, "Echallens", Source.CIVILE, true, adresses.representation);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.TUTELLE, false, adresses.poursuiteAutreTiers); // adresse du tuteur

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Madame", domicile.getLigne1());
		assertEquals("Anabela Lopes", domicile.getLigne2());
		assertEquals("Grand Rue", domicile.getLigne3());
		assertEquals("1040 Echallens", domicile.getLigne4());
		assertNull(domicile.getLigne5());

		final AdresseEnvoi courrier = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
		assertEquals("Madame", courrier.getLigne1());
		assertEquals("Anabela Lopes", courrier.getLigne2());
		assertEquals("p.a. OTG", courrier.getLigne3());
		assertEquals("Chemin de Mornex 32", courrier.getLigne4());
		assertEquals("1014 Lausanne", courrier.getLigne5());
		assertNull(courrier.getLigne6());

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false));

		final AdresseEnvoiDetaillee poursuiteAutreTiers = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false);
		assertEquals("Office Tuteur général", poursuiteAutreTiers.getLigne1());
		assertEquals("Chemin de Mornex 32", poursuiteAutreTiers.getLigne2());
		assertEquals("1014 Lausanne", poursuiteAutreTiers.getLigne3());
		assertNull(poursuiteAutreTiers.getLigne4());
		assertEquals(Source.TUTELLE, poursuiteAutreTiers.getSource());
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableSousTutelleAvecAutoriteTutelaire() throws Exception {

		final long noTiers = 60510843;
		final long noIndividu = 750946;

		// un contribuable sous tutelle avec l'OTG (Lausanne) comme tuteur et l'autorité tutelaire de Yverdon et environs
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1953, 11, 2), "Lopes", "Anabela", false);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 1, 1), null);
			}
		});

		final PersonnePhysique ctb = addHabitant(noTiers, noIndividu);
		final CollectiviteAdministrative tuteur = addCollAdm(ServiceInfrastructureService.noTuteurGeneral);
		final CollectiviteAdministrative autoriteTutelaire = addCollAdm(MockCollectiviteAdministrative.JusticePaix.DistrictsJuraNordVaudoisEtGrosDeVaud.getNoColAdm());
		addTutelle(ctb, tuteur, autoriteTutelaire, date(2000, 1, 1), null);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Echallens", Source.CIVILE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.TUTELLE, false, adresses.courrier); // adresse du tuteur
		assertAdresse(date(2000, 1, 1), null, "Yverdon-les-Bains", Source.TUTELLE, false, adresses.poursuite); // adresse de l'autorité tutelaire
		assertAdresse(date(2000, 1, 1), null, "Echallens", Source.CIVILE, true, adresses.representation);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.TUTELLE, false, adresses.poursuiteAutreTiers); // adresse du tuteur

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Madame", domicile.getLigne1());
		assertEquals("Anabela Lopes", domicile.getLigne2());
		assertEquals("Grand Rue", domicile.getLigne3());
		assertEquals("1040 Echallens", domicile.getLigne4());
		assertNull(domicile.getLigne5());

		final AdresseEnvoi courrier = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
		assertEquals("Madame", courrier.getLigne1());
		assertEquals("Anabela Lopes", courrier.getLigne2());
		assertEquals("p.a. OTG", courrier.getLigne3());
		assertEquals("Chemin de Mornex 32", courrier.getLigne4());
		assertEquals("1014 Lausanne", courrier.getLigne5());
		assertNull(courrier.getLigne6());

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false));

		final AdresseEnvoi poursuite = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false);
		assertEquals("Justice de Paix des districts du", poursuite.getLigne1());
		assertEquals("Jura-Nord Vaudois et du Gros-de-Vaud", poursuite.getLigne2());
		assertEquals("Rue du Pré 2", poursuite.getLigne3());
		assertEquals("Case Postale 693", poursuite.getLigne4());
		assertEquals("1400 Yverdon-les-Bains", poursuite.getLigne5());
		assertNull(poursuite.getLigne6());

		final AdresseEnvoiDetaillee poursuiteAutreTiers = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false);
		assertEquals("Office Tuteur général", poursuiteAutreTiers.getLigne1());
		assertEquals("Chemin de Mornex 32", poursuiteAutreTiers.getLigne2());
		assertEquals("1014 Lausanne", poursuiteAutreTiers.getLigne3());
		assertNull(poursuiteAutreTiers.getLigne4());
		assertEquals(Source.TUTELLE, poursuiteAutreTiers.getSource());
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableHSAvecRepresentantConventionel() throws Exception {

		final long noTiers = 10536395;
		final long noEntreprise = MockPersonneMorale.KPMG.getNumeroEntreprise();
		
		servicePM.setUp(new DefaultMockServicePM());

		// un contribuable hors-Suisse avec un représentant conventionnel en Suisse (avec extension de l'exécution forcée)
		final PersonnePhysique ctb = addNonHabitant(noTiers, "Claude-Alain", "Proz", date(1953, 11, 2), Sexe.MASCULIN);
		addAdresseEtrangere(ctb, TypeAdresseTiers.DOMICILE, date(2000, 1, 1), null, null, "Izmir", MockPays.Turquie);
		final Entreprise representant = addEntreprise(noEntreprise);
		addRepresentationConventionnelle(ctb, representant, date(2000, 1, 1), true);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Izmir", Source.FISCALE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.REPRESENTATION, false, adresses.courrier); // adresse du représentant
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.REPRESENTATION, false, adresses.poursuite); // adresse du représentant
		assertAdresse(date(2000, 1, 1), null, "Izmir", Source.FISCALE, true, adresses.representation);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.REPRESENTATION, false, adresses.poursuiteAutreTiers); // adresse du représentant

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Monsieur", domicile.getLigne1());
		assertEquals("Claude-Alain Proz", domicile.getLigne2());
		assertEquals("Izmir", domicile.getLigne3());
		assertEquals("Turquie", domicile.getLigne4());
		assertNull(domicile.getLigne5());

		final AdresseEnvoi courrier = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
		assertEquals("Monsieur", courrier.getLigne1());
		assertEquals("Claude-Alain Proz", domicile.getLigne2());
		assertEquals("p.a. KPMG SA", courrier.getLigne3());
		assertEquals("Avenue de Rumine 37", courrier.getLigne4());
		assertEquals("1005 Lausanne", courrier.getLigne5());
		assertNull(courrier.getLigne6());

		final AdresseEnvoiDetaillee representation = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false);
		assertEquals(domicile, representation);

		final AdresseEnvoi poursuite = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false);
		assertEquals("KPMG SA", poursuite.getLigne1());
		assertEquals("Avenue de Rumine 37", poursuite.getLigne2());
		assertEquals("1005 Lausanne", poursuite.getLigne3());
		assertNull(poursuite.getLigne4());

		final AdresseEnvoiDetaillee poursuiteAutreTiers = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false);
		assertEquals(poursuite, poursuiteAutreTiers);
		assertEquals(Source.REPRESENTATION, poursuiteAutreTiers.getSource());
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableVDAvecRepresentantConventionel() throws Exception {

		final long noTiers = 10033975;
		final long noIndividu = 330581;
		final long noEntreprise = MockPersonneMorale.CuriaTreuhand.getNumeroEntreprise();

		servicePM.setUp(new DefaultMockServicePM());

		// un contribuable vaudois avec un représentant conventionnel en Suisse (sans extension de l'exécution forcée qui n'est pas autorisée dans ce cas-là)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1953, 11, 2), "Pesci-Mouttet", "Marcello", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lonay.CheminDuRechoz, null, date(2000, 1, 1), null);
			}
		});

		final PersonnePhysique ctb = addHabitant(noTiers, noIndividu);
		final Entreprise representant = addEntreprise(noEntreprise);
		addRepresentationConventionnelle(ctb, representant, date(2000, 1, 1), false);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Lonay", Source.CIVILE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Chur", Source.REPRESENTATION, false, adresses.courrier); // adresse du représentant
		assertAdresse(date(2000, 1, 1), null, "Lonay", Source.CIVILE, false, adresses.poursuite); // adresse du contribuable parce que pas d'exécution forcée
		assertAdresse(date(2000, 1, 1), null, "Lonay", Source.CIVILE, true, adresses.representation);
		assertNull(adresses.poursuiteAutreTiers);

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Monsieur", domicile.getLigne1());
		assertEquals("Marcello Pesci-Mouttet", domicile.getLigne2());
		assertEquals("Chemin de Réchoz", domicile.getLigne3());
		assertEquals("1027 Lonay", domicile.getLigne4());
		assertNull(domicile.getLigne5());

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false));
		
		final AdresseEnvoi courrier = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
		assertEquals("Monsieur", courrier.getLigne1());
		assertEquals("Marcello Pesci-Mouttet", courrier.getLigne2());
		assertEquals("p.a. Curia Treuhand AG", courrier.getLigne3());
		assertEquals("Grabenstrasse 15", courrier.getLigne4());
		assertEquals("7000 Chur", courrier.getLigne5());
		assertNull(courrier.getLigne6());

		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false));
		assertNull(adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false));
	}

	/**
	 * Voir la spécification "BesoinsContentieux.doc"
	 */
	@Test
	public void testGetAdressesPoursuiteContribuableAvecAdresseSpecifiquePoursuite() throws Exception {

		final long noTiers = 44018109;
		final long noIndividu = 381865;

		servicePM.setUp(new DefaultMockServicePM());

		// un contribuable vaudois avec une adresse de poursuite entrée dans Unireg
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1953, 11, 2), "Galley", "Philippe", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.GrangesMarnand.SousLeBois, null, date(2000, 1, 1), null);
			}
		});

		final PersonnePhysique ctb = addHabitant(noTiers, noIndividu);
		addAdresseSuisse(ctb, TypeAdresseTiers.POURSUITE, date(2000,1,1), null, MockRue.Lausanne.CheminPrazBerthoud);

		// les adresses fiscales
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(ctb, null, false);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, false, adresses.domicile);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, true, adresses.courrier);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.FISCALE, false, adresses.poursuite);
		assertAdresse(date(2000, 1, 1), null, "Granges-près-Marnand", Source.CIVILE, true, adresses.representation);
		assertAdresse(date(2000, 1, 1), null, "Lausanne", Source.FISCALE, false, adresses.poursuiteAutreTiers);

		// les adresses d'envoi
		final AdresseEnvoi domicile = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.DOMICILE, false);
		assertEquals("Monsieur", domicile.getLigne1());
		assertEquals("Philippe Galley", domicile.getLigne2());
		assertEquals("Chemin Sous le Bois", domicile.getLigne3());
		assertEquals("1523 Granges-près-Marnand", domicile.getLigne4());
		assertNull(domicile.getLigne5());
		
		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false));
		assertEquals(domicile, adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.REPRESENTATION, false));

		final AdresseEnvoi poursuite = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE, false);
		assertEquals("Monsieur", poursuite.getLigne1());
		assertEquals("Philippe Galley", poursuite.getLigne2());
		assertEquals("Chemin de Praz-Berthoud", poursuite.getLigne3());
		assertEquals("1000 Lausanne", poursuite.getLigne4());
		assertNull(poursuite.getLigne5());

		final AdresseEnvoiDetaillee poursuiteAutreTiers = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, false);
		assertEquals(poursuite, poursuiteAutreTiers);
		assertEquals(Source.FISCALE, poursuiteAutreTiers.getSource());
	}

	/**
	 * Vérifie que les rapports entre tiers annulés ne sont pas pris en compte.
	 */
	@Test
	public void testGetAdressesFiscalesRapportsAnnules() throws Exception {

		final PersonnePhysique representant = addNonHabitant("Marcel", "Espol", date(1923, 3, 2), Sexe.MASCULIN);
		addAdresseSuisse(representant, TypeAdresseTiers.COURRIER, date(1923, 3, 2), null, MockRue.Bussigny.RueDeLIndustrie);
		final PersonnePhysique represente = addNonHabitant("Julius", "Raeb", date(1923, 3, 2), Sexe.MASCULIN);
		addAdresseSuisse(represente, TypeAdresseTiers.COURRIER, date(1923, 3, 2), null, MockRue.GrangesMarnand.RueDeVerdairuz);

		final RepresentationConventionnelle rapport = addRepresentationConventionnelle(represente, representant, date(2000, 1, 1), false);
		rapport.setAnnule(true);

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(represente, null, true);
		assertAdresse(date(1923, 3, 2), null, "Granges-près-Marnand", Source.FISCALE, false, adresses.courrier);

		final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(represente, true);
		assertEquals(1, adressesHisto.courrier.size());
		assertAdresse(date(1923, 3, 2), null, "Granges-près-Marnand", Source.FISCALE, false, adressesHisto.courrier.get(0));
	}

	/**
	 * [UNIREG-2654] Vérifie que la surcharge des adresses fonctionne correctement lorsqu'une adresse poursuite fermée existe en même temps qu'une adresse courrier valide sur toute la période.
	 */
	@Test
	public void testGetAdressesFiscalesAvecAdressePoursuiteFermee() throws Exception {

		final DebiteurPrestationImposable debiteur = addDebiteur();
		addAdresseSuisse(debiteur, TypeAdresseTiers.COURRIER, date(2010, 1, 1), null, MockRue.Lausanne.BoulevardGrancy);
		addAdresseSuisse(debiteur, TypeAdresseTiers.POURSUITE, date(2010, 5, 1), date(2010, 5, 22), MockRue.Echallens.GrandRue);

		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, true);
		assertNotNull(adresses);

		assertEquals(1, adresses.courrier.size());
		assertAdresse(date(2010, 1, 1), null, "Lausanne", Source.FISCALE, false, adresses.courrier.get(0));

		assertEquals(3, adresses.poursuite.size());
		assertAdresse(date(2010, 1, 1), date(2010, 4, 30), "Lausanne", Source.FISCALE, true, adresses.poursuite.get(0));
		assertAdresse(date(2010, 5, 1), date(2010, 5, 22), "Echallens", Source.FISCALE, false, adresses.poursuite.get(1));
		assertAdresse(date(2010, 5, 23), null, "Lausanne", Source.FISCALE, true, adresses.poursuite.get(2));
	}

	/**
	 * [UNIREG-2688] On s'assure que la source de l'adresse 'poursuite autre tiers' est bien CURATELLE dans le cas d'une curatelle dont les adresses de début et de fin sont nulles.
	 */
	@Test
	public void testGetAdressesFiscalesPersonnePhysiqueAvecCuratelleDatesDebutFinNulles() throws Exception {

		final long noIndividuTiia = 339619;
		final long noIndividuSylvie = 339618;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu tiia = addIndividu(noIndividuTiia, date(1989, 12, 21), "Tauxe", "Tiia", false);
				addAdresse(tiia, EnumTypeAdresse.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, EnumTypeAdresse.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, EnumTypeAdresse.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, EnumTypeAdresse.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(tiia, EnumTypeAdresse.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);

				MockIndividu sylvie = addIndividu(noIndividuSylvie, date(1955, 9, 19), "Tauxe", "Sylvie", false);
				addAdresse(sylvie, EnumTypeAdresse.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, EnumTypeAdresse.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, EnumTypeAdresse.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, EnumTypeAdresse.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(sylvie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
			}
		});

		class Ids {
			long tiia;
			long sylvie;
		}
		final Ids ids = new Ids();

		validationInterceptor.setEnabled(false); // pour permettre l'ajout d'une curatelle avec date de début nulle
		try {
			doInNewTransactionAndSession(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					PersonnePhysique tiia = addHabitant(noIndividuTiia);
					addAdresseSuisse(tiia, TypeAdresseTiers.COURRIER, date(2009, 7, 8), null, MockRue.Lausanne.PlaceSaintFrancois);
					ids.tiia = tiia.getId();
					PersonnePhysique sylvie = addHabitant(noIndividuSylvie);
					ids.sylvie = sylvie.getId();
					addCuratelle(tiia, sylvie, null, null);
					return null;
				}
			});
		}
		finally {
			validationInterceptor.setEnabled(true);
		}

		final PersonnePhysique tiia = (PersonnePhysique) tiersDAO.get(ids.tiia);
		assertNotNull(tiia);

		{
			final AdressesFiscales adresses = adresseService.getAdressesFiscales(tiia, null, true);
			assertNotNull(adresses);
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CURATELLE, false, adresses.courrier);
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CIVILE, false, adresses.representation);
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CIVILE, false, adresses.domicile);
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CIVILE, false, adresses.poursuite);
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CURATELLE, false, adresses.poursuiteAutreTiers);
		}

		{
			final AdressesFiscalesHisto adressesHisto = adresseService.getAdressesFiscalHisto(tiia, true);
			assertNotNull(adressesHisto);

			assertEquals(3, adressesHisto.courrier.size());
			assertAdresse(null, date(2006, 9, 24), "Moudon", Source.CURATELLE, false, adressesHisto.courrier.get(0));
			assertAdresse(date(2006, 9, 25), date(2009, 1, 31), "Pully", Source.CURATELLE, false, adressesHisto.courrier.get(1));
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CURATELLE, false, adressesHisto.courrier.get(2));

			assertEquals(3, adressesHisto.representation.size());
			assertAdresse(null, date(2006, 9, 24), "Moudon", Source.CIVILE, false, adressesHisto.representation.get(0));
			assertAdresse(date(2006, 9, 25), date(2009, 1, 31), "Pully", Source.CIVILE, false, adressesHisto.representation.get(1));
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.representation.get(2));

			assertEquals(3, adressesHisto.domicile.size());
			assertAdresse(null, date(2006, 9, 24), "Moudon", Source.CIVILE, false, adressesHisto.domicile.get(0));
			assertAdresse(date(2006, 9, 25), date(2009, 1, 31), "Pully", Source.CIVILE, false, adressesHisto.domicile.get(1));
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.domicile.get(2));

			assertEquals(3, adressesHisto.poursuite.size());
			assertAdresse(null, date(2006, 9, 24), "Moudon", Source.CIVILE, false, adressesHisto.poursuite.get(0));
			assertAdresse(date(2006, 9, 25), date(2009, 1, 31), "Pully", Source.CIVILE, false, adressesHisto.poursuite.get(1));
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CIVILE, false, adressesHisto.poursuite.get(2));

			assertEquals(3, adressesHisto.poursuiteAutreTiers.size());
			assertAdresse(null, date(2006, 9, 24), "Moudon", Source.CURATELLE, false, adressesHisto.poursuiteAutreTiers.get(0));
			assertAdresse(date(2006, 9, 25), date(2009, 1, 31), "Pully", Source.CURATELLE, false, adressesHisto.poursuiteAutreTiers.get(1));
			assertAdresse(date(2009, 2, 1), null, "Lausanne", Source.CURATELLE, false, adressesHisto.poursuiteAutreTiers.get(2));
		}
	}

	@Test
	public void testIsPrefixedByPourAdresse() {

		// Case de base
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse(""));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("pa Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Pa Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("PA Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p/a Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P/a Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P/A Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("pa. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Pa. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("PA. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.a Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.a. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Chez Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("CHEZ Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/ Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/ Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/o Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/o Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/O Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/O Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("cO Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Co Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("CO Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("cO. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Co. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("CO. Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ems Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/ems Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/Ems Martine Dupont"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/EMS Martine Dupont"));

		// Cas tordus
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("parloir 12"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chezelle-le-haut"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Route de P.A. DuMoulin"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Route de Chezelle"));

		// Quelques cas vus sur le Host
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("P/Giobellina E")); // que signifie P/ ???
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("P. Matthey"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A. G. Mayer-Suter"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A. Zosso SA"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A. Fid.Favre SA"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.a. Mandataria Fiduciaire SA"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("P/sté Fid Suisse, M. Pidoux")); // que signifie P/ ???
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A. M. François Martin"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("P/Ofisa/Dupont")); // que signifie P/ ???
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.a.:Uditintercom"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.A. Fid. Guex S.A."));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("P.a.:George Mettrau curateur"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("PA. MK Gestion"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Pin d'Arolle"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Paul Aebi"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Pousaz-Gmur Rosa"));

		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("pa Coloñia Condessa"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Dr. Bruhin & Partner"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Treuhand Werthmüller"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Fiduciaire Tucker"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Studer & Plüss"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Innovation GmbH"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Schellenberg Wittmer"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Fiscompta S.A."));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("p PBBG Gérances & Gestion Immo")); // erreur de saisie -> on ne peut deviner
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Sté Fiduciaire Suisse"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Henchoz Henri"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Union de Banques Suisses"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Union de Banques Suisses"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Cossy Renée"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("p.a. Thurin Ch.-H"));

		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez M. Daniel BENEY"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Mme Zermatten"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Mme Zermatten"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Mme Lidia Aiello"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Mme Lidia Aiello"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Résidence du Léman"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Résidence du Léman"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Mme N. Pfeiffer"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez M. Claude Magliocco"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez M. Claude Magliocco"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez M. Magliocco"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez M. Magliocco"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Stübi Corinne"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("chez Stübi Corinne"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("chef de	service	CH & BE"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("chef de	service	CH & BE"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("chef de	service	CH & BE"));

		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chevalley"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chalet Boschi"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chr. Favre"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chr. Favre"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chalet Mon Loisir"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chalet Arc-en-Ciel"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chemin du Dérochet 2"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Chez Jean-Louis de Bourgues"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Chez Jean-Louis de Bourgues"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Chez Mercy Ships"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("Chefarzt, Chirurgische Abteil."));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("Chez Résidence du Léman"));

		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ Mme Gertrud Prepper"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ Monsanto International"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ Norbert Julen"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/o david et Sophie Oguey"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/o ses parents"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/o A. GUIGNARD"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/o A. Horisberger"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/. Brügger Valérie"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/. Ch. et I. Gabeel"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/. Ch. et I. Gaberel"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/. Dominique Fournier"));

		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co André Sohaing"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co EMS Hôpital d'Aubonne"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co Jean Maurice David"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co Jean-Maurice David"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("co Mme Florence  Perez"));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("co-loc. Naef E."));
		assertFalse(AdresseServiceImpl.isPrefixedByPourAdresse("cordey marcel"));

		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("c/ems victoria-residence"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/EMS des Novalles"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/EMS l'Ours"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/Ems du Jura"));
		assertTrue(AdresseServiceImpl.isPrefixedByPourAdresse("C/Ems les 4 saisons"));
	}

	/**
	 * [UNIREG-1398] Types de tiers qui n'ont pas de formule de politesse
	 */
	@Test
	public void testGetFormulePolitesseTiersSansFormule() {

		assertNull(adresseService.getFormulePolitesse(new AutreCommunaute()));
		assertNull(adresseService.getFormulePolitesse(new CollectiviteAdministrative()));
		assertNull(adresseService.getFormulePolitesse(new Entreprise()));
		assertNull(adresseService.getFormulePolitesse(new Etablissement()));
		assertNull(adresseService.getFormulePolitesse(new DebiteurPrestationImposable()));
	}

	/**
	 * [UNIREG-1398] Cas des personnes physiques
	 */
	@Test
	public void testGetFormulePolitessePersonnesPhysiques() {

		PersonnePhysique pp = new PersonnePhysique(false);
		pp.setSexe(Sexe.MASCULIN);
		assertEquals(FormulePolitesse.MONSIEUR, adresseService.getFormulePolitesse(pp));

		pp.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.MADAME, adresseService.getFormulePolitesse(pp));

		pp.setSexe(null);
		assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(pp));
	}

	/**
	 * [UNIREG-1398] Cas des personnes physiques décédées
	 */
	@Test
	public void testGetFormulePolitessePersonnePhysiquesDecedees() {

		PersonnePhysique pp = new PersonnePhysique(false);
		pp.setDateDeces(date(2000,1,1));
		pp.setSexe(Sexe.MASCULIN);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(pp));

		pp.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(pp));

		pp.setSexe(null);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(pp));
	}

	/**
	 * [UNIREG-1398] Cas des ménages communs
	 */
	@Test
	public void testGetFormulePolitesseMenageCommuns() {

		PersonnePhysique pp1 = addNonHabitant(null, "pp-un", date(1977, 1, 1), Sexe.MASCULIN);
		PersonnePhysique pp2 = addNonHabitant(null, "pp-deux", date(1977, 1, 1), Sexe.MASCULIN);
		EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), null);
		MenageCommun mc = ensemble.getMenage();

		// couple mixte
		pp1.setSexe(Sexe.MASCULIN);
		pp2.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.MONSIEUR_ET_MADAME, adresseService.getFormulePolitesse(mc));

		// couple mixte (variante)
		pp1.setSexe(Sexe.FEMININ);
		pp2.setSexe(Sexe.MASCULIN);
		assertEquals(FormulePolitesse.MONSIEUR_ET_MADAME, adresseService.getFormulePolitesse(mc));

		// couple homosexuel
		pp1.setSexe(Sexe.MASCULIN);
		pp2.setSexe(Sexe.MASCULIN);
		assertEquals(FormulePolitesse.MESSIEURS, adresseService.getFormulePolitesse(mc));

		// couple homosexuel féminin
		pp1.setSexe(Sexe.FEMININ);
		pp2.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.MESDAMES, adresseService.getFormulePolitesse(mc));

		// couples partiellement indéterminés (les 4 variantes)
		pp1.setSexe(Sexe.MASCULIN);
		pp2.setSexe(null);
		assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.FEMININ);
		pp2.setSexe(null);
		assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(null);
		pp2.setSexe(Sexe.MASCULIN);
		assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(null);
		pp2.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(mc));

		// couple complétement indéterminé
		pp1.setSexe(null);
		pp2.setSexe(null);
		assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(mc));
	}

	/**
	 * [UNIREG-1398] Cas des ménages communs avec un ou plusieurs composants décédés
	 */
	@Test
	public void testGetFormulePolitesseMenageCommunsAvecDecedes() {

		PersonnePhysique pp1 = addNonHabitant(null, "pp-un", date(1977, 1, 1), Sexe.MASCULIN);
		PersonnePhysique pp2 = addNonHabitant(null, "pp-deux", date(1977, 1, 1), Sexe.MASCULIN);
		EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp1, pp2, date(2000, 1, 1), null);
		MenageCommun mc = ensemble.getMenage();

		// couple mixte (les 3 variantes)
		pp1.setSexe(Sexe.MASCULIN);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.MASCULIN);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(Sexe.FEMININ);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		// couple homosexuel
		pp1.setSexe(Sexe.MASCULIN);
		pp2.setSexe(Sexe.MASCULIN);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.MASCULIN);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(Sexe.MASCULIN);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		// couple homosexuel féminin
		pp1.setSexe(Sexe.FEMININ);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(Sexe.FEMININ);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.FEMININ);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(Sexe.FEMININ);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		// couples partiellement indéterminés
		pp1.setSexe(Sexe.MASCULIN);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(null);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.MASCULIN);
		pp2.setSexe(null);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.FEMININ);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(null);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(Sexe.FEMININ);
		pp2.setSexe(null);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		// couple complétement indéterminé
		pp1.setSexe(null);
		pp1.setDateDeces(date(2000, 1, 1));
		pp2.setSexe(null);
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));

		pp1.setSexe(null);
		pp2.setSexe(null);
		pp2.setDateDeces(date(2000, 1, 1));
		assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(mc));
	}

	private void assertAdressesByTypeEquals(final AdressesFiscales adresses, Tiers tiers, RegDate date) throws AdresseException {
		assertAdressesEquals(adresses.courrier, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, date, false));
		assertAdressesEquals(adresses.representation, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.REPRESENTATION, date, false));
		assertAdressesEquals(adresses.poursuite, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.POURSUITE, date, false));
		assertAdressesEquals(adresses.domicile, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.DOMICILE, date, false));
	}

	@Test
	public void testGetTypeAffranchissement() throws Exception {

		final PersonnePhysique jean = addNonHabitant("Jean", "Lavanchy", date(1967, 5, 12), Sexe.MASCULIN);
		addAdresseSuisse(jean, TypeAdresseTiers.REPRESENTATION, date(2000, 1, 1), null, MockRue.Chamblon.GrandRue);
		addAdresseEtrangere(jean, TypeAdresseTiers.DOMICILE, date(2000, 1, 1), null, "Grand-Rue 23", "23000 Ciboulette", MockPays.France);
		addAdresseEtrangere(jean, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, "Rue de la poudre de merlin-pinpin", "4444 Bogotta", MockPays.Colombie);

		final AdressesFiscales adresses = adresseService.getAdressesFiscales(jean, null, true);
		final AdresseGenerique adresseSuisse = adresses.representation;
		final AdresseGenerique adresseFrance = adresses.domicile;
		final AdresseGenerique adresseColombie = adresses.courrier;

		assertEquals(TypeAffranchissement.SUISSE, adresseService.getTypeAffranchissement(adresseSuisse));
		assertEquals(TypeAffranchissement.EUROPE, adresseService.getTypeAffranchissement(adresseFrance));
		assertEquals(TypeAffranchissement.MONDE, adresseService.getTypeAffranchissement(adresseColombie));
	}

	/**
	 * [UNIREG-2792] Un contribuable avec une représentation qui commence (1er janvier 2009) avant la date de début de son adresse courrier (8 juillet 2009) : l'adresse courrier définie doit être
	 * complétement masquée par l'adresse du représentant <b>mais</b> elle doit quand même être utilisée pour calculer les défauts des adresses domicile, poursuite, ...
	 */
	@Test
	public void testGetAdressesContribuableAvecRepresentationQuiCommenceAvantSonAdresseCourrier() throws Exception {

		final PersonnePhysique sophie = addNonHabitant("Sophie", "Regamey", date(1960, 1, 1), Sexe.FEMININ);
		addAdresseSuisse(sophie, TypeAdresseTiers.COURRIER, date(2009, 7, 8), null, MockRue.Zurich.GloriaStrasse);

		final PersonnePhysique anne = addNonHabitant("Anne", "Bolomey", date(1960, 1, 1), Sexe.FEMININ);
		addAdresseSuisse(anne, TypeAdresseTiers.DOMICILE, date(1992, 4, 27), null, MockRue.Moudon.LeBourg);

		addRepresentationConventionnelle(sophie, anne, date(2009, 1, 1), false);

		// adresses du jour
		assertAdresse(date(2009, 1, 1), null, "Moudon", Source.REPRESENTATION, false, adresseService.getAdresseFiscale(sophie, TypeAdresseFiscale.COURRIER, null, true));
		final AdresseGenerique domicile = adresseService.getAdresseFiscale(sophie, TypeAdresseFiscale.DOMICILE, null, true);
		assertAdresse(date(2009, 7, 8), null, "Zurich", Source.FISCALE, true, domicile);
		assertAdressesEquals(domicile, adresseService.getAdresseFiscale(sophie, TypeAdresseFiscale.POURSUITE, null, true));
		assertAdressesEquals(domicile, adresseService.getAdresseFiscale(sophie, TypeAdresseFiscale.REPRESENTATION, null, true));
		
		// adresses historiques
		final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(sophie, true);
		assertNotNull(adresses);
		assertEquals(1, adresses.courrier.size());
		assertEquals(1, adresses.domicile.size());
		assertEquals(1, adresses.poursuite.size());
		assertEquals(1, adresses.representation.size());
		assertAdresse(date(2009, 1, 1), null, "Moudon", Source.REPRESENTATION, false, adresses.courrier.get(0));
		assertAdresse(date(2009, 7, 8), null, "Zurich", Source.FISCALE, true, adresses.domicile.get(0));
		assertAdressesEquals(adresses.domicile.get(0), adresses.poursuite.get(0));
		assertAdressesEquals(adresses.domicile.get(0), adresses.representation.get(0));
	}
}

package ch.vd.uniregctb.adresse;

import static ch.vd.uniregctb.adresse.AdresseTestCase.assertAdresse;
import static ch.vd.uniregctb.adresse.AdresseTestCase.assertAdressesEquals;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ch.vd.uniregctb.tiers.*;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class AdresseServiceTest extends BusinessTest {

	private AdresseService adresseService;
	private TiersService tiersService;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		// Pas d'indexation parce qu'on teste des cas qui font peter l'indexation
		// et qui pourrissent les logs!
		globalTiersIndexer.setOnTheFlyIndexation(false);

		// Instanciation du service à la main pour pouvoir taper dans les méthodes protégées.
		adresseService = new AdresseServiceImpl(tiersService, serviceInfra, servicePM, serviceCivil);
	}

	@Override
	public void onTearDown() throws Exception {
		globalTiersIndexer.setOnTheFlyIndexation(true);
		super.onTearDown();
	}

	@Test
	public void testGetAdressesFiscalNonHabitantAvecDomicileSeulement() throws Exception {

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

		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseTiers.COURRIER, null, false));
		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseTiers.DOMICILE, null, false));
		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseTiers.POURSUITE, null, false));
		assertNotNull(adresseService.getAdresseFiscale(nonhabitant, TypeAdresseTiers.REPRESENTATION, null, false));
	}

	@Test
	public void testGetAdressesFiscalHistoNonHabitantAvecDomicileSeulement() throws Exception {

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
	public void testGetAdressesFiscalHistoSansTiers() throws Exception {
		assertNull(adresseService.getAdressesFiscales(null, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseTiers.COURRIER, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseTiers.DOMICILE, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseTiers.POURSUITE, date(2000, 1, 1), false));
		assertNull(adresseService.getAdresseFiscale(null, TypeAdresseTiers.REPRESENTATION, date(2000, 1, 1), false));
	}

	@Test
	public void testGetAdressesFiscalHistoSansAdresseFiscale() throws Exception {

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
	public void testGetAdressesFiscalHistoCasGeneral() throws Exception {

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
	public void testGetAdressesFiscalHistoSurchargeCivil() throws Exception {
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
	public void testGetAdressesFiscalHistoSurchargeAutreTiers() throws Exception {
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
			adresse.setAutreTiers(autreHabitant);
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
	public void testGetAdressesFiscalHistoCasParticulier1() throws Exception {
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
	public void testGetAdressesFiscalHistoCasParticulier2() throws Exception {
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
	public void testGetAdressesFiscalHistoCasParticulier3() throws Exception {
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
	public void testGetAdressesFiscalHistoDonneesIncoherentesPlusieursPrincipales() throws Exception {
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
				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, true);
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
	public void testGetAdressesFiscalHistoDonneesIncoherentesDatesDebutEtFinInversees() throws Exception {
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
				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(habitant, true);
				fail();
			}
			catch (AdresseException e) {
				assertEquals("adresse civile courrier :\nLa date de début [2020.01.01] ne peut pas être après la date de fin [2000.03.19].", e.getMessage());
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
	public void testGetAdressesFiscalAvecAdresseAnnulee() throws Exception {
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
	public void testGetAdressesFiscalHistoMenageCommun() throws Exception {
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
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2004, 7, 14), null);
				menage = (MenageCommun) rapport.getObjet();
				long noMenageCommun = menage.getNumero();
				return noMenageCommun;
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
	 * Cas particulier du ménage composé de non-habitants (donc sans adresse civiles) et avec un principal qui possède une adresse courrier
	 * définie au niveau fiscal.
	 */
	@Test
	public void testGetAdressesFiscalHistoMenageCommunNonHabitants() throws Exception {

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
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2004, 7, 14), null);
				menage = (MenageCommun) rapport.getObjet();
				long noMenageCommun = menage.getNumero();
				return noMenageCommun;
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
	public void testGetAdressesFiscalHistoTutelle() throws Exception {
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
				rapport.setDateDebut(date(2004, 01, 01));
				rapport.setDateFin(date(2007, 12, 31));
				rapport.setObjet(tuteur);
				rapport.setSujet(pupille);
				tiersDAO.save(rapport);

				long numeroContribuablePupille = pupille.getNumero();
				return numeroContribuablePupille;
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
	public void testGetAdressesFiscalHistoCoupleTutellePrincipal() throws Exception {
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
				menage = (MenageCommun) rapport.getObjet();
				numeros.numeroContribuableMenage = menage.getNumero();
				principal = (PersonnePhysique) rapport.getSujet();
				numeros.numeroContribuablePrincipal = principal.getNumero();

				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2000, 1, 1), null);
				conjoint = (PersonnePhysique) rapport.getSujet();
				numeros.numeroContribuableConjoint = conjoint.getNumero();

				// Crée le tuteur
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// Crée la tutelle proprement dites
				rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 01, 01));
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
	public void testGetAdressesFiscalHistoCoupleTutelleConjoint() throws Exception {
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
				menage = (MenageCommun) rapport.getObjet();
				numeros.numeroContribuableMenage = menage.getNumero();
				principal = (PersonnePhysique) rapport.getSujet();
				numeros.numeroContribuablePrincipal = principal.getNumero();

				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2000, 1, 1), null);
				conjoint = (PersonnePhysique) rapport.getSujet();
				numeros.numeroContribuableConjoint = conjoint.getNumero();

				// Crée le tuteur
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// Crée la tutelle proprement dites
				rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 01, 01));
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
	public void testGetAdressesFiscalHistoCoupleTutellePrincipalEtConjoint() throws Exception {
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
				menage = (MenageCommun) rapport.getObjet();
				numeros.numeroContribuableMenage = menage.getNumero();
				principal = (PersonnePhysique) rapport.getSujet();
				numeros.numeroContribuablePrincipal = principal.getNumero();

				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2000, 1, 1), null);
				conjoint = (PersonnePhysique) rapport.getSujet();
				numeros.numeroContribuableConjoint = conjoint.getNumero();

				// Crée la tutelle sur le principal
				PersonnePhysique tuteurPrincipal = new PersonnePhysique(true);
				tuteurPrincipal.setNumeroIndividu(noTuteurPrincipal);
				tuteurPrincipal = (PersonnePhysique) tiersDAO.save(tuteurPrincipal);

				rapport = new Tutelle();
				rapport.setDateDebut(date(2004, 01, 01));
				rapport.setDateFin(date(2007, 12, 31));
				rapport.setObjet(tuteurPrincipal);
				rapport.setSujet(principal);
				tiersDAO.save(rapport);

				// Crée la tutelle sur le conjoint
				PersonnePhysique tuteurConjoint = new PersonnePhysique(true);
				tuteurConjoint.setNumeroIndividu(noTuteurConjoint);
				tuteurConjoint = (PersonnePhysique) tiersDAO.save(tuteurConjoint);

				rapport = new Tutelle();
				rapport.setDateDebut(date(2005, 07, 01));
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
			assertAdresse(date(2000, 1, 1), date(2005, 06, 30), "Bex", Source.CIVILE, true, adresses.courrier.get(0));
			assertAdresse(date(2005, 07, 01), date(2009, 12, 31), "Clées, Les", Source.TUTELLE, false, adresses.courrier.get(1));
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
	public void testGetAdressesFiscalDetectionRecursionInfinie() throws Exception {
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
				adresse.setAutreTiers(pupille);
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

				long numeroContribuablePupille = pupille.getNumero();
				return numeroContribuablePupille;
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
				adresseService.getAdresseFiscale(pupille, TypeAdresseTiers.COURRIER, date(2000, 1, 1), false);
				fail();
			}
			catch (AdressesResolutionException ok) {
				// ok
			}
			adresseService.getAdresseFiscale(pupille, TypeAdresseTiers.REPRESENTATION, date(2000, 1, 1), false);
			adresseService.getAdresseFiscale(pupille, TypeAdresseTiers.POURSUITE, date(2000, 1, 1), false);
			adresseService.getAdresseFiscale(pupille, TypeAdresseTiers.DOMICILE, date(2000, 1, 1), false);

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
	public void testGetAdressesFiscalHistoAvecAdresseCivile() throws Exception {

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
	public void testGetAdressesFiscalHistoDefaultAdressesPrincipalCivil() throws Exception {

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
	public void testGetAdressesFiscalHistoDefaultAdressesCourrierCivil() throws Exception {

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
	public void testGetAdressesFiscalHistoEntrepriseSansAdresseFiscale() throws Exception {

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
			entreprise = (Entreprise) tiersDAO.save(entreprise);
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
			assertEquals("Monsieur et Madame", service.getFormulePolitesse(ensemble).salutations());
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
			assertEquals("Mesdames", service.getFormulePolitesse(ensemble).salutations());
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
			assertEquals("Messieurs", service.getFormulePolitesse(ensemble).salutations());
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
			assertEquals("Madame, Monsieur", service.getFormulePolitesse(ensemble).salutations());
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
			assertEquals("Madame, Monsieur", service.getFormulePolitesse(ensemble).salutations());
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
			assertEquals("Madame, Monsieur", service.getFormulePolitesse(ensemble).salutations());
		}
	}

	/**
	 * Teste qu'il est possible de récupérer les adresses d'un débiteur sans contribuable associé.
	 */
	@Test
	public void testGetAdresseFiscalDebiteurPrestationImposableSansContribuableAssocie() throws Exception {

		// Crée un habitant et un débiteur associé
		final long noDebiteur = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				debiteur.setNom1("Arnold Schwarz");
				debiteur.setComplementNom("Ma petite entreprise");
				debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
				addAdresseSuisse(debiteur, TypeAdresseTiers.COURRIER, date(1980,1,1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				return debiteur.getNumero();
			}
		});

		{
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(debiteur);

			// Vérification des adresses ponctuelles
			{
				final AdressesFiscales adresses1950 = adresseService.getAdressesFiscales(debiteur, date(1950, 1, 1), false);
				assertNotNull(adresses1950);
				assertNull(adresses1950.courrier);
				assertNull(adresses1950.representation);
				assertNull(adresses1950.poursuite);
				assertNull(adresses1950.domicile);

				assertAdressesByTypeEquals(adresses1950, debiteur, date(1950, 1, 1));
			}

			{
				final AdressesFiscales adresses1982 = adresseService.getAdressesFiscales(debiteur, date(1982, 1, 1), false);
				assertNotNull(adresses1982);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, false, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, adresses1982.representation);
				assertAdressesEquals(adresses1982.representation, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.representation, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(1, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, false, adresses.courrier.get(0));
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.FISCALE, true, adresses.representation.get(0));
				assertAdressesEquals(adresses.representation, adresses.poursuite);
				assertAdressesEquals(adresses.representation, adresses.domicile);
			}
		}
	}

	/**
	 * Teste qu'un débiteur hérite bien des adresses du contribuable associé.
	 */
	@Test
	public void testGetAdresseFiscalDebiteurPrestationImposableSansAdresseSurPersonnePhysique() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant et un débiteur associé
		final long noDebiteur = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				debiteur.setComplementNom("Ma petite entreprise");
				debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
				long noDebiteur = debiteur.getNumero();

				PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(noIndividu);
				habitant = (PersonnePhysique) tiersDAO.save(habitant);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, habitant, debiteur);
				tiersDAO.getHibernateTemplate().merge(contact);

				return noDebiteur;
			}
		});

		{
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(debiteur);

			// Vérification des adresses ponctuelles
			{
				final AdressesFiscales adresses1950 = adresseService.getAdressesFiscales(debiteur, date(1950, 1, 1), false);
				assertNotNull(adresses1950);
				assertNull(adresses1950.courrier);
				assertNull(adresses1950.representation);
				assertNull(adresses1950.poursuite);
				assertNull(adresses1950.domicile);

				assertAdressesByTypeEquals(adresses1950, debiteur, date(1950, 1, 1));
			}

			{
				final AdressesFiscales adresses1982 = adresseService.getAdressesFiscales(debiteur, date(1982, 1, 1), false);
				assertNotNull(adresses1982);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.CONTRIBUABLE, true, adresses1982.courrier);
				assertAdressesEquals(adresses1982.courrier, adresses1982.representation);
				assertAdressesEquals(adresses1982.courrier, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.courrier, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(1, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.CONTRIBUABLE, true, adresses.courrier.get(0));

				assertAdressesEquals(adresses.courrier, adresses.representation);
				assertAdressesEquals(adresses.courrier, adresses.poursuite);
				assertAdressesEquals(adresses.poursuite, adresses.domicile);
			}
		}
	}

	@Test
	public void testGetAdresseEnvoiDebiteurPrestationImposableAvecAdresseCourrierSurPersonnePhysique() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant et un débiteur associé
		final long noDebiteur = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				{
					debiteur.setComplementNom("Ma petite entreprise");
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setNumeroMaison("2bis");
					adresse.setDateDebut(date(1990, 1, 1));
					adresse.setDateFin(null);
					adresse.setUsage(TypeAdresseTiers.COURRIER);
					adresse.setNumeroRue(MockRue.CossonayVille.CheminDeRiondmorcel.getNoRue());
					adresse.setNumeroOrdrePoste(MockLocalite.CossonayVille.getNoOrdre());
					debiteur.addAdresseTiers(adresse);
				}
				debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
				long noDebiteur = debiteur.getNumero();

				PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(noIndividu);
				habitant = (PersonnePhysique) tiersDAO.save(habitant);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, habitant, debiteur);
				tiersDAO.getHibernateTemplate().merge(contact);

				return noDebiteur;
			}
		});

		{
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(debiteur);

			// Vérification des adresses ponctuelles
			{
				final AdressesFiscales adresses1950 = adresseService.getAdressesFiscales(debiteur, date(1950, 1, 1), false);
				assertNotNull(adresses1950);
				assertNull(adresses1950.courrier);
				assertNull(adresses1950.representation);
				assertNull(adresses1950.poursuite);
				assertNull(adresses1950.domicile);

				assertAdressesByTypeEquals(adresses1950, debiteur, date(1950, 1, 1));
			}

			{
				final AdressesFiscales adresses1982 = adresseService.getAdressesFiscales(debiteur, date(1982, 1, 1), false);
				assertNotNull(adresses1982);
				// FIXME (msi) cette adresse devrait être fermée au 31 décembre 1989 !
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.CONTRIBUABLE, true, adresses1982.courrier);
				assertAdressesEquals(adresses1982.courrier, adresses1982.representation);
				assertAdressesEquals(adresses1982.courrier, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.courrier, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			{
				final AdressesFiscales adresses1990 = adresseService.getAdressesFiscales(debiteur, date(1990, 1, 1), false);
				assertNotNull(adresses1990);
				assertAdresse(date(1990, 1, 1), null, "Cossonay-Ville", Source.FISCALE, false, adresses1990.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.CONTRIBUABLE, true, adresses1990.poursuite);
				assertAdressesEquals(adresses1990.poursuite, adresses1990.representation);
				assertAdressesEquals(adresses1990.poursuite, adresses1990.domicile);

				assertAdressesByTypeEquals(adresses1990, debiteur, date(1990, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(2, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), date(1989, 12, 31), "Lausanne", Source.CONTRIBUABLE, true, adresses.courrier.get(0));
				assertAdresse(date(1990, 1, 1), null, "Cossonay-Ville", Source.FISCALE, false, adresses.courrier.get(1));

				assertEquals(1, adresses.poursuite.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", Source.CONTRIBUABLE, true, adresses.poursuite.get(0));
				assertAdressesEquals(adresses.poursuite, adresses.representation);
				assertAdressesEquals(adresses.poursuite, adresses.domicile);
			}
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
	public void testGetAdressesMenageCommunAvecRapportsAnnules() throws Exception {

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
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, date(2004, 7, 14), null);
				rapport.setAnnule(true);
				menage = (MenageCommun) rapport.getObjet();
				long noMenageCommun = menage.getNumero();
				return noMenageCommun;
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

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, date(2007,3,3), TypeAdresseTiers.COURRIER, false);
		assertNotNull(adresseEnvoi);
		assertNull(adresseEnvoi.getLigne1());
		assertNull(adresseEnvoi.getLigne2());
		assertNull(adresseEnvoi.getLigne3());
		assertNull(adresseEnvoi.getLigne4());
		assertNull(adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
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
	 * [UNIREG-1398]
	 */
	@Test
	public void testGetFormulePolitesse() {

		// Tous ces types de tiers n'ont pas de formule de politesse
		assertNull(adresseService.getFormulePolitesse(new AutreCommunaute()));
		assertNull(adresseService.getFormulePolitesse(new CollectiviteAdministrative()));
		assertNull(adresseService.getFormulePolitesse(new Entreprise()));
		assertNull(adresseService.getFormulePolitesse(new Etablissement()));
		assertNull(adresseService.getFormulePolitesse(new DebiteurPrestationImposable()));

		// Cas des personnes physiques
		{
			PersonnePhysique pp = new PersonnePhysique(false);
			pp.setSexe(Sexe.MASCULIN);
			assertEquals(FormulePolitesse.MONSIEUR, adresseService.getFormulePolitesse(pp));

			pp.setSexe(Sexe.FEMININ);
			assertEquals(FormulePolitesse.MADAME, adresseService.getFormulePolitesse(pp));

			pp.setSexe(null);
			assertEquals(FormulePolitesse.MADAME_MONSIEUR, adresseService.getFormulePolitesse(pp));
		}

		// Cas des personnes physiques décédées
		{
			PersonnePhysique pp = new PersonnePhysique(false);
			pp.setDateDeces(date(2000,1,1));
			pp.setSexe(Sexe.MASCULIN);
			assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(pp));

			pp.setSexe(Sexe.FEMININ);
			assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(pp));

			pp.setSexe(null);
			assertEquals(FormulePolitesse.HERITIERS, adresseService.getFormulePolitesse(pp));
		}

		// Cas des ménages communs
		{
			PersonnePhysique pp1 = new PersonnePhysique(false);
			pp1.setNom("pp1");
			PersonnePhysique pp2 = new PersonnePhysique(false);
			pp2.setNom("pp2");
			MenageCommun mc = new MenageCommun();
			final AppartenanceMenage rpp1 = new AppartenanceMenage(date(2000,1,1), null, pp1, mc);
			pp1.addRapportSujet(rpp1);
			mc.addRapportObjet(rpp1);
			final AppartenanceMenage rpp2 = new AppartenanceMenage(date(2000,1,1), null, pp2, mc);
			pp2.addRapportSujet(rpp2);
			mc.addRapportObjet(rpp2);

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

		// Cas des ménages communs avec un ou plusieurs composants décédés
		{
			PersonnePhysique pp1 = new PersonnePhysique(false);
			pp1.setNom("pp1");
			PersonnePhysique pp2 = new PersonnePhysique(false);
			pp2.setNom("pp2");
			MenageCommun mc = new MenageCommun();
			final AppartenanceMenage rpp1 = new AppartenanceMenage(date(2000,1,1), null, pp1, mc);
			pp1.addRapportSujet(rpp1);
			mc.addRapportObjet(rpp1);
			final AppartenanceMenage rpp2 = new AppartenanceMenage(date(2000,1,1), null, pp2, mc);
			pp2.addRapportSujet(rpp2);
			mc.addRapportObjet(rpp2);

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
	}

	private void assertAdressesByTypeEquals(final AdressesFiscales adresses, Tiers tiers, RegDate date) throws AdresseException {
		assertAdressesEquals(adresses.courrier, adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.COURRIER, date, false));
		assertAdressesEquals(adresses.representation, adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.REPRESENTATION, date, false));
		assertAdressesEquals(adresses.poursuite, adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.POURSUITE, date, false));
		assertAdressesEquals(adresses.domicile, adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.DOMICILE, date, false));
	}

	@Test
	public void testAdresseCurateurDeMadameHabitanteAvecMonsieurNonHabitant() throws Exception {

		// test créé pour le cas jira UNIREG-1954

		final long noIndividuMadame = 12345L;
		final long noIndividuCurateurMadame = 12346L;
		final long noIndividuTuteurMonsieur = 3245L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// voilà madame
				final MockIndividu albertine = addIndividu(noIndividuMadame, date(1954, 5, 2), "Pittet", "Albertine", false);
				addAdresse(albertine, EnumTypeAdresse.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null, date(1980, 1, 1), null);
				addAdresse(albertine, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, date(1980, 1, 1), null);

				// c'est le curateur (de madame)
				final MockIndividu pierre = addIndividu(noIndividuCurateurMadame, date(1953, 11, 2), "Dupont", "Pierre", true);
				addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);

				// et le tuteur (de monsieur)
				final MockIndividu nicolas = addIndividu(noIndividuTuteurMonsieur, date(1940, 1, 15), "Ricola", "Nicolas", true);
				addAdresse(nicolas, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null, date(1980, 1, 1), null);
				addAdresse(nicolas, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1980, 1, 1), null);
			}
		});

		final PersonnePhysique monsieur = addNonHabitant("Achille", "Talon", date(1963,11,7), Sexe.MASCULIN);
		final PersonnePhysique madame = addHabitant(noIndividuMadame);
		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(2000, 1, 1));
		final MenageCommun mc = ensemble.getMenage();

		final PersonnePhysique tuteurMonsieur = addHabitant(noIndividuTuteurMonsieur);
		final Tutelle tutelle = new Tutelle(date(2000,1,1), null, monsieur, tuteurMonsieur);
		monsieur.addRapportSujet(tutelle);
		tuteurMonsieur.addRapportObjet(tutelle);

		final PersonnePhysique curateurMadame = addHabitant(noIndividuCurateurMadame);
		final Curatelle curatelle = new Curatelle(date(2000,1,1), null, madame, curateurMadame);
		madame.addRapportSujet(curatelle);
		curateurMadame.addRapportObjet(curatelle);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(mc, null, TypeAdresseTiers.COURRIER, true);
		Assert.assertNotNull(adresseEnvoi);
		Assert.assertEquals("Pierre Dupont est le curateur de Madame, seule habitante du couple", "p.a. Pierre Dupont", adresseEnvoi.getPourAdresse());
	}
}

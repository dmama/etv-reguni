package ch.vd.unireg.adresse;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;

import static ch.vd.unireg.adresse.AdresseTestCase.assertAdresse;
import static ch.vd.unireg.adresse.AdresseTestCase.assertAdressesEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Teste du service des adresses spécialisé pour les débiteurs de prestations imposables
 */
@SuppressWarnings({"JavaDoc"})
public class AdresseServiceDebiteurTest extends BusinessTest {

	private AdresseService adresseService;
	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private LocaliteInvalideMatcherService localiteInvalideMatcherService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		localiteInvalideMatcherService = getBean(LocaliteInvalideMatcherService.class, "localiteInvalideMatcherService");

		// Pas d'indexation parce qu'on teste des cas qui font peter l'indexation et qui pourrissent les logs!
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(false);

		// Instanciation du service à la main pour pouvoir taper dans les méthodes protégées.
		adresseService = new AdresseServiceImpl(tiersService, tiersDAO, serviceInfra, serviceEntreprise, serviceCivil, localiteInvalideMatcherService);
	}

	@Override
	public void onTearDown() throws Exception {
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAdressesDebiteurAvecAdresseCourrierSurPersonnePhysiqueAvecAdresseCourrier() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);
				MockAdresse adresse = addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant et un débiteur associé
		final long noDebiteur = doInNewTransaction(status -> {
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
			long no = debiteur.getNumero();

			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);

			ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, habitant, debiteur);
			hibernateTemplate.merge(contact);
			return no;
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
				assertAdresse(date(1980, 1, 1), date(1989, 12, 31), "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses1982.representation);
				assertAdressesEquals(adresses1982.representation, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.representation, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			{
				final AdressesFiscales adresses1990 = adresseService.getAdressesFiscales(debiteur, date(1990, 1, 1), false);
				assertNotNull(adresses1990);
				assertAdresse(date(1990, 1, 1), null, "Cossonay-Ville", AdresseGenerique.SourceType.FISCALE, false, adresses1990.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses1990.poursuite);
				assertAdressesEquals(adresses1990.poursuite, adresses1990.representation);
				assertAdressesEquals(adresses1990.poursuite, adresses1990.domicile);

				assertAdressesByTypeEquals(adresses1990, debiteur, date(1990, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(2, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), date(1989, 12, 31), "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses.courrier.get(0));
				assertAdresse(date(1990, 1, 1), null, "Cossonay-Ville", AdresseGenerique.SourceType.FISCALE, false, adresses.courrier.get(1));

				assertEquals(1, adresses.poursuite.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses.poursuite.get(0));
				assertAdressesEquals(adresses.poursuite, adresses.representation);
				assertAdressesEquals(adresses.poursuite, adresses.domicile);
			}
		}
	}

	/**
	 * [UNIREG-2896] Vérifie que les adresses du débiteur (même celles par défaut) sont utilisées lorsque le contribuable associé ne possède pas d'adresse.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAdressesDebiteurAvecAdressePoursuiteSurPersonnePhysiqueSansAdresse() throws Exception {

		// Crée un non-habitant sans adresse et un débiteur associé avec adresse de poursuite
		final long noDebiteur = doInNewTransaction(status -> {
			final PersonnePhysique arnold = addNonHabitant("Arnold", "Whitenegger", date(1960, 1, 1), Sexe.FEMININ);
			final DebiteurPrestationImposable debiteur = addDebiteur("Ma petite entreprise", arnold, date(1980, 1, 1));
			addAdresseSuisse(debiteur, TypeAdresseTiers.POURSUITE, date(1980, 1, 1), null, MockRue.Bussigny.RueDeLIndustrie);
			return debiteur.getNumero();
		});

		{
			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(debiteur);

			// Les adresses fiscales
			final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, true);
			assertNotNull(adresses);
			assertEquals(1, adresses.courrier.size());
			assertEquals(1, adresses.domicile.size());
			assertEquals(1, adresses.poursuite.size());
			assertEquals(1, adresses.representation.size());

			assertAdresse(date(1980, 1, 1), null, "Bussigny", AdresseGenerique.SourceType.FISCALE, true, adresses.courrier.get(0));
			assertAdresse(date(1980, 1, 1), null, "Bussigny", AdresseGenerique.SourceType.FISCALE, true, adresses.domicile.get(0));
			assertAdresse(date(1980, 1, 1), null, "Bussigny", AdresseGenerique.SourceType.FISCALE, false, adresses.poursuite.get(0));
			assertAdresse(date(1980, 1, 1), null, "Bussigny", AdresseGenerique.SourceType.FISCALE, true, adresses.representation.get(0));

			// Les adresses d'envoi
			final AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, true);
			assertNotNull(adresseCourrier);
			assertEquals("Arnold Whitenegger", adresseCourrier.getLigne1());
			assertEquals("Ma petite entreprise", adresseCourrier.getLigne2());
			assertEquals("Rue de l'Industrie", adresseCourrier.getLigne3());
			assertEquals("1030 Bussigny", adresseCourrier.getLigne4());
			assertNull(adresseCourrier.getLigne5());
			assertNull(adresseCourrier.getLigne6());
		}
	}

	/**
	 * Teste qu'un débiteur hérite bien des adresses du contribuable associé.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAdressesDebiteurSansAdresseSurPersonnePhysique() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);
				MockAdresse adresse = addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant et un débiteur associé
		final long noDebiteur = doInNewTransaction(status -> {
			DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
			debiteur.setComplementNom("Ma petite entreprise");
			debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
			long no = debiteur.getNumero();

			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);
			habitant = (PersonnePhysique) tiersDAO.save(habitant);

			ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, habitant, debiteur);
			hibernateTemplate.merge(contact);
			return no;
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
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses1982.courrier);
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
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.CONTRIBUABLE, true, adresses.courrier.get(0));

				assertAdressesEquals(adresses.courrier, adresses.representation);
				assertAdressesEquals(adresses.courrier, adresses.poursuite);
				assertAdressesEquals(adresses.poursuite, adresses.domicile);
			}
		}
	}

	/**
	 * Teste qu'il est possible de récupérer les adresses d'un débiteur sans contribuable associé.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAdressesDebiteurSansContribuableAssocie() throws Exception {

		// Crée un habitant et un débiteur associé
		final long noDebiteur = doInNewTransaction(status -> {
			DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
			debiteur.setNom1("Arnold Schwarz");
			debiteur.setComplementNom("Ma petite entreprise");
			debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
			addAdresseSuisse(debiteur, TypeAdresseTiers.COURRIER, date(1980, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
			return debiteur.getNumero();
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
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.FISCALE, false, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.FISCALE, true, adresses1982.representation);
				assertAdressesEquals(adresses1982.representation, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.representation, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(1, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.FISCALE, false, adresses.courrier.get(0));
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.SourceType.FISCALE, true, adresses.representation.get(0));
				assertAdressesEquals(adresses.representation, adresses.poursuite);
				assertAdressesEquals(adresses.representation, adresses.domicile);
			}
		}
	}

	private void assertAdressesByTypeEquals(final AdressesFiscales adresses, Tiers tiers, RegDate date) throws AdresseException {
		assertAdressesEquals(adresses.courrier, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, date, false));
		assertAdressesEquals(adresses.representation, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.REPRESENTATION, date, false));
		assertAdressesEquals(adresses.poursuite, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.POURSUITE, date, false));
		assertAdressesEquals(adresses.domicile, adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.DOMICILE, date, false));
	}

	/**
	 * [SIFISC-12400] les mentions "Aux héritiers de" et ", défunt(e)" doivent également apparaître dans les adresses du débiteur
	 */
	@Test
	public void testAdresseDebiteurAvecReferentDecede() throws Exception {

		// mise en place civile (= vide)
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place fiscale
		final long dpiId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alphonse", "Baudet", null, Sexe.MASCULIN);
			pp.setDateDeces(date(2014, 6, 12));
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.CossonayVille.AvenueDuFuniculaire);

			final DebiteurPrestationImposable dpi = addDebiteur("MonComplément", pp, date(2006, 1, 1));
			return dpi.getNumero();
		});

		// demande de l'adresse du débiteur
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(dpi, null, TypeAdresseFiscale.COURRIER, false);
			Assert.assertEquals("Aux héritiers de", adresse.getLigne1());
			Assert.assertEquals("Alphonse Baudet, défunt", adresse.getLigne2());
			Assert.assertEquals("MonComplément", adresse.getLigne3());
			Assert.assertEquals("Avenue du Funiculaire", adresse.getLigne4());
			Assert.assertEquals("1304 Cossonay-Ville", adresse.getLigne5());
			Assert.assertNull(adresse.getLigne6());
			return null;
		});
	}
}

package ch.vd.uniregctb.adresse;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static ch.vd.uniregctb.adresse.AdresseTestCase.assertAdresse;
import static ch.vd.uniregctb.adresse.AdresseTestCase.assertAdressesEquals;
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

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		// Pas d'indexation parce qu'on teste des cas qui font peter l'indexation et qui pourrissent les logs!
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
	public void testAdressesDebiteurAvecAdresseCourrierSurPersonnePhysiqueAvecAdresseCourrier() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);
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
				assertAdresse(date(1980, 1, 1), date(1989, 12, 31), "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses1982.representation);
				assertAdressesEquals(adresses1982.representation, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.representation, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			{
				final AdressesFiscales adresses1990 = adresseService.getAdressesFiscales(debiteur, date(1990, 1, 1), false);
				assertNotNull(adresses1990);
				assertAdresse(date(1990, 1, 1), null, "Cossonay-Ville", AdresseGenerique.Source.FISCALE, false, adresses1990.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses1990.poursuite);
				assertAdressesEquals(adresses1990.poursuite, adresses1990.representation);
				assertAdressesEquals(adresses1990.poursuite, adresses1990.domicile);

				assertAdressesByTypeEquals(adresses1990, debiteur, date(1990, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(2, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), date(1989, 12, 31), "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses.courrier.get(0));
				assertAdresse(date(1990, 1, 1), null, "Cossonay-Ville", AdresseGenerique.Source.FISCALE, false, adresses.courrier.get(1));

				assertEquals(1, adresses.poursuite.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses.poursuite.get(0));
				assertAdressesEquals(adresses.poursuite, adresses.representation);
				assertAdressesEquals(adresses.poursuite, adresses.domicile);
			}
		}
	}

	/**
	 * [UNIREG-2896] Vérifie que les adresses du débiteur (même celles par défaut) sont utilisées lorsque le contribuable associé ne possède pas d'adresse.
	 */
	@Test
	public void testAdressesDebiteurAvecAdressePoursuiteSurPersonnePhysiqueSansAdresse() throws Exception {

		// Crée un non-habitant sans adresse et un débiteur associé avec adresse de poursuite
		final long noDebiteur = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique arnold = addNonHabitant("Arnold", "Whitenegger", date(1960, 1, 1), Sexe.FEMININ);
				final DebiteurPrestationImposable debiteur = addDebiteur("Ma petite entreprise", arnold, date(1980, 1, 1));
				addAdresseSuisse(debiteur, TypeAdresseTiers.POURSUITE, date(1980, 1, 1), null, MockRue.Bussigny.RueDeLIndustrie);

				return debiteur.getNumero();
			}
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

			assertAdresse(date(1980, 1, 1), null, "Bussigny-près-Lausanne", AdresseGenerique.Source.FISCALE, true, adresses.courrier.get(0));
			assertAdresse(date(1980, 1, 1), null, "Bussigny-près-Lausanne", AdresseGenerique.Source.FISCALE, true, adresses.domicile.get(0));
			assertAdresse(date(1980, 1, 1), null, "Bussigny-près-Lausanne", AdresseGenerique.Source.FISCALE, false, adresses.poursuite.get(0));
			assertAdresse(date(1980, 1, 1), null, "Bussigny-près-Lausanne", AdresseGenerique.Source.FISCALE, true, adresses.representation.get(0));

			// Les adresses d'envoi
			final AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, true);
			assertNotNull(adresseCourrier);
			assertEquals("Arnold Whitenegger", adresseCourrier.getLigne1());
			assertEquals("Ma petite entreprise", adresseCourrier.getLigne2());
			assertEquals("Rue de l'Industrie", adresseCourrier.getLigne3());
			assertEquals("1030 Bussigny-près-Lausanne", adresseCourrier.getLigne4());
			assertNull(adresseCourrier.getLigne5());
			assertNull(adresseCourrier.getLigne6());
		}
	}

	/**
	 * Teste qu'un débiteur hérite bien des adresses du contribuable associé.
	 */
	@Test
	public void testAdressesDebiteurSansAdresseSurPersonnePhysique() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, date(1953, 11, 2), "Dupont", "Pierre", true);
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);
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
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses1982.courrier);
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
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.CONTRIBUABLE, true, adresses.courrier.get(0));

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
	public void testAdressesDebiteurSansContribuableAssocie() throws Exception {

		// Crée un habitant et un débiteur associé
		final long noDebiteur = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				debiteur.setNom1("Arnold Schwarz");
				debiteur.setComplementNom("Ma petite entreprise");
				debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
				addAdresseSuisse(debiteur, TypeAdresseTiers.COURRIER, date(1980, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

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
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.FISCALE, false, adresses1982.courrier);
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.FISCALE, true, adresses1982.representation);
				assertAdressesEquals(adresses1982.representation, adresses1982.poursuite);
				assertAdressesEquals(adresses1982.representation, adresses1982.domicile);

				assertAdressesByTypeEquals(adresses1982, debiteur, date(1982, 1, 1));
			}

			// Vérification des adresses historiques
			{

				final AdressesFiscalesHisto adresses = adresseService.getAdressesFiscalHisto(debiteur, false);
				assertNotNull(adresses);

				assertEquals(1, adresses.courrier.size());
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.FISCALE, false, adresses.courrier.get(0));
				assertAdresse(date(1980, 1, 1), null, "Lausanne", AdresseGenerique.Source.FISCALE, true, adresses.representation.get(0));
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
}

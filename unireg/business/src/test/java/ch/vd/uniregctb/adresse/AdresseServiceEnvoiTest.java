package ch.vd.uniregctb.adresse;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class AdresseServiceEnvoiTest extends BusinessTest {

	private AdresseService adresseService;
	private TiersService tiersService;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testGetAdresseEnvoiPersonnePhysiqueSansComplementNiCasePostale() throws Exception {
		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		final long noHabitant;
		{
			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);

			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			noHabitant = habitant.getNumero();
		}

		{
			PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(noHabitant);
			assertNotNull(habitant);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne3());
			assertEquals("1000 Lausanne", adresse.getLigne4());
			assertNull(adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur", adresse.getSalutations());
			assertEquals("Monsieur", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.POURSUITE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.REPRESENTATION, false));

			List<String> nomCourrier = adresseService.getNomCourrier(habitant, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
		}
	}

	@Test
	public void testGetAdresseEnvoiCoupleSansComplementNiCasePostale() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu marie = addIndividu(noIndividuConjoint, RegDate.get(1959, 3, 14), "Dupont", "Marie", false);

				marieIndividus(pierre, marie, RegDate.get(1980, 1, 1));

				// adresses courriers
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("3bis");
			}
		});

		long noMenageCommun = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, RegDate.get(2004, 7, 14), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				return menage.getNumero();
			}
		});

		{
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);
			assertNotNull(menage);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur et Madame", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("Marie Dupont", adresse.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne4());
			assertEquals("1000 Lausanne", adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur et Madame", adresse.getSalutations());
			assertEquals("Monsieur et Madame", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));

			List<String> nomCourrier = adresseService.getNomCourrier(menage, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Marie Dupont", nomCourrier.get(1));
		}
	}

	/**
	 * [UNIREG-2234] Vérifie que l'adresse complète du couple est disponible, même après la date de séparation/divorce
	 */
	@Test
	public void testGetAdresseEnvoiCoupleApresDivorce() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu marie = addIndividu(noIndividuConjoint, RegDate.get(1959, 3, 14), "Dupont", "Marie", false);

				marieIndividus(pierre, marie, RegDate.get(1980, 1, 1));

				// adresses courriers
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.BoulevardGrancy, null, RegDate.get(1980, 1, 1), null);
			}
		});

		long noMenageCommun = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée un ménage composé de deux habitants divorcé en 2004
				PersonnePhysique principal = addHabitant(noIndividuPrincipal);
				PersonnePhysique conjoint = addHabitant(noIndividuConjoint);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(1994, 7, 14), date(2004, 7, 14));
				return ensemble.getMenage().getNumero();
			}
		});

		{
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);
			assertNotNull(menage);

			// Adresse d'envoi sans mention de date (= dernier état connu)
			final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
			assertNotNull(adresse);
			assertEquals("Monsieur et Madame", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("Marie Dupont", adresse.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne4());
			assertEquals("1000 Lausanne", adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur et Madame", adresse.getSalutations());
			assertEquals("Monsieur et Madame", adresse.getFormuleAppel());

			// Toutes les adresses d'envoi (avant mariage, pendant et après) du couple doivent être les mêmes en l'occurence
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, date(1980, 1, 1), TypeAdresseFiscale.COURRIER, true)); // avant
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, date(2000, 1, 1), TypeAdresseFiscale.COURRIER, true)); // pendant
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, date(2010, 1, 1), TypeAdresseFiscale.COURRIER, true)); // après
		}
	}

	@Test
	public void testGetAdresseEnvoiPersonnePhysiqueAvecComplement() throws Exception {
		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
				adresse.setTitre("chez Popol");
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		final long noHabitant;
		{
			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);

			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			noHabitant = habitant.getNumero();
		}

		{
			PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(noHabitant);
			assertNotNull(habitant);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("chez Popol", adresse.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne4());
			assertEquals("1000 Lausanne", adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur", adresse.getSalutations());
			assertEquals("Monsieur", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(habitant, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
		}
	}

	@Test
	public void testGetAdresseEnvoiPersonnePhysiqueAvecCasePostale() throws Exception {
		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						"Case Postale 144", RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		final long noHabitant;
		{
			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);

			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			noHabitant = habitant.getNumero();
		}

		{
			PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(noHabitant);
			assertNotNull(habitant);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne3());
			assertEquals("Case Postale 144", adresse.getLigne4());
			assertEquals("1000 Lausanne", adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur", adresse.getSalutations());
			assertEquals("Monsieur", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(habitant, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
		}
	}

	@Test
	public void testGetAdresseEnvoiPersonnePhysiqueAvecComplementEtCasePostale() throws Exception {
		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						"Case Postale 144", RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
				adresse.setTitre("chez Popol");
			}
		});

		// Crée un habitant sans adresse fiscale surchargée
		final long noHabitant;
		{
			PersonnePhysique habitant = new PersonnePhysique(true);
			habitant.setNumeroIndividu(noIndividu);

			habitant = (PersonnePhysique) tiersDAO.save(habitant);
			noHabitant = habitant.getNumero();
		}

		{
			PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(noHabitant);
			assertNotNull(habitant);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("chez Popol", adresse.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne4());
			assertEquals("Case Postale 144", adresse.getLigne5());
			assertEquals("1000 Lausanne", adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur", adresse.getSalutations());
			assertEquals("Monsieur", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(habitant, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
		}
	}

	@Test
	public void testGetAdresseEnvoiCoupleAvecComplementEtCasePostale() throws Exception {

		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu marie = addIndividu(noIndividuConjoint, RegDate.get(1959, 3, 14), "Dupont", "Marie", false);

				marieIndividus(pierre, marie, RegDate.get(1980, 1, 1));

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						"Case Postale 144", RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
				adresse.setTitre("chez Popol");
			}
		});

		long noMenageCommun = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, RegDate.get(2004, 7, 14), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				return menage.getNumero();
			}
		});

		{
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);
			assertNotNull(menage);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur et Madame", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("Marie Dupont", adresse.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne4());
			assertEquals("Case Postale 144", adresse.getLigne5());
			assertEquals("1000 Lausanne", adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur et Madame", adresse.getSalutations());
			assertEquals("Monsieur et Madame", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(menage, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Marie Dupont", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiDebiteurPrestationImposableSansAdresseSurPersonnePhysique() throws Exception {

		final long noIndividu = 1;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		final long noDebiteur = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée un habitant et un débiteur associé
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

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Pierre Dupont", adresse.getLigne1());
			assertEquals("Ma petite entreprise", adresse.getLigne2());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne3());
			assertEquals("1000 Lausanne", adresse.getLigne4());
			assertNull(adresse.getLigne5());
			assertTrue(adresse.isSuisse());
			assertNull(adresse.getSalutations());
			assertNull(adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Ma petite entreprise", nomCourrier.get(1));
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
				MockIndividu pierre = addIndividu(noIndividu, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// adresses courriers
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
				adresse.setNumero("3bis");
			}
		});

		// Crée un habitant et un débiteur associé
		final long noDebiteur = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				{
					debiteur.setComplementNom("Ma petite entreprise");
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setNumeroMaison("2bis");
					adresse.setDateDebut(RegDate.get(1980, 1, 1));
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

			AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresseCourrier);
			assertEquals("Pierre Dupont", adresseCourrier.getLigne1());
			assertEquals("Ma petite entreprise", adresseCourrier.getLigne2());
			assertEquals("Chemin de Riondmorcel 2bis", adresseCourrier.getLigne3());
			assertEquals("1304 Cossonay-Ville", adresseCourrier.getLigne4());
			assertNull(adresseCourrier.getLigne5());
			assertTrue(adresseCourrier.isSuisse());
			assertNull(adresseCourrier.getSalutations());
			assertNull(adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Pierre Dupont", adresseDomicile.getLigne1());
			assertEquals("Ma petite entreprise", adresseDomicile.getLigne2());
			assertEquals("Av de Beaulieu 3bis", adresseDomicile.getLigne3());
			assertEquals("1000 Lausanne", adresseDomicile.getLigne4());
			assertNull(adresseDomicile.getLigne5());
			assertTrue(adresseDomicile.isSuisse());
			assertNull(adresseDomicile.getSalutations());
			assertNull(adresseDomicile.getFormuleAppel());

			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Ma petite entreprise", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiDebiteurPrestationImposableSurCollectiviteAdministrative() throws Exception {

		final int noTribunalCantonal = 1;

		serviceInfra.setUp(new MockServiceInfrastructureService() {
			@Override
			protected void init() {
				pays.add(MockPays.Suisse);
				cantons.add(MockCanton.Vaud);
				communesVaud.add(MockCommune.Lausanne);
				localites.add(MockLocalite.Lausanne);
				rues.add(MockRue.Lausanne.AvenueDeBeaulieu);

				MockCollectiviteAdministrative tribunalCantonal = new MockCollectiviteAdministrative();
				tribunalCantonal.setNoColAdm(noTribunalCantonal);
				tribunalCantonal.setNomComplet1("Tribunal cantonal");
				tribunalCantonal.setNomComplet2("Palais de justice de l'Hermitage");
				tribunalCantonal.setNomComplet3(null);
				tribunalCantonal.setNomCourt("TRIBUNAL CANTONAL VD");
				MockAdresse adresse = new MockAdresse();
				adresse.setRue("Route du Signal 8");
				adresse.setNumeroPostal("1014");
				adresse.setLocalite("Lausanne");
				adresse.setDateDebutValidite(null);
				adresse.setTypeAdresse(TypeAdresseCivil.COURRIER);
				adresse.setCommuneAdresse(MockCommune.Lausanne);
				tribunalCantonal.setAdresse(adresse);
				collectivitesAdministrative.add(tribunalCantonal);
			}
		});

		final long noDebiteur = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée un habitant et un débiteur associé
				DebiteurPrestationImposable tribunal = new DebiteurPrestationImposable();
				tribunal.setComplementNom("Service du Personnel");
				tribunal = (DebiteurPrestationImposable) tiersDAO.save(tribunal);
				long noDebiteur = tribunal.getNumero();

				CollectiviteAdministrative tribunalCantonal = new CollectiviteAdministrative();
				tribunalCantonal.setNumeroCollectiviteAdministrative(noTribunalCantonal);
				tribunalCantonal = (CollectiviteAdministrative) tiersDAO.save(tribunalCantonal);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, tribunalCantonal, tribunal);
				tiersDAO.getHibernateTemplate().merge(contact);

				return noDebiteur;
			}
		});

		{
			DebiteurPrestationImposable tribunal = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(tribunal);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Tribunal cantonal", adresse.getLigne1());
			assertEquals("Palais de justice de l'Hermitage", adresse.getLigne2());
			assertEquals("Service du Personnel", adresse.getLigne3());
			assertEquals("Route du Signal 8", adresse.getLigne4());
			assertEquals("1014 Lausanne", adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertNull(adresse.getSalutations());
			assertNull(adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(tribunal, null, false);
			assertNotNull(nomCourrier);
			assertEquals(3, nomCourrier.size());
			assertEquals("Tribunal cantonal", nomCourrier.get(0));
			assertEquals("Palais de justice de l'Hermitage", nomCourrier.get(1));
			assertEquals("Service du Personnel", nomCourrier.get(2));
		}
	}

	@Test
	public void testGetAdresseEnvoiDebiteurPrestationImposableSurEntreprise() throws Exception {

		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				addPM(MockPersonneMorale.NestleSuisse);
			}
		});

		final long noDebiteur = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Crée un habitant et un débiteur associé
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				debiteur.setComplementNom("Usine d'Orbe");
				debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
				long noDebiteur = debiteur.getNumero();

				Entreprise nestleSuisse = new Entreprise();
				nestleSuisse.setNumero(270123L);
				nestleSuisse.setNumeroEntreprise(MockPersonneMorale.NestleSuisse.getNumeroEntreprise());
				nestleSuisse = (Entreprise) tiersDAO.save(nestleSuisse);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nestleSuisse, debiteur);
				tiersDAO.getHibernateTemplate().merge(contact);

				return noDebiteur;
			}
		});

		{
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(debiteur);

			AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresseCourrier);
			assertEquals("Nestlé Suisse S.A.", adresseCourrier.getLigne1());
			assertEquals("Usine d'Orbe", adresseCourrier.getLigne2());
			assertEquals("Finance et Audit", adresseCourrier.getLigne3());
			assertEquals("pa Myriam Steiner / CP 352", adresseCourrier.getLigne4()); // il s'agit de la rue, cette adresse est très mal définie dans la base, en fait.
			assertEquals("1800 Vevey", adresseCourrier.getLigne5());
			assertNull(adresseCourrier.getLigne6());
			assertTrue(adresseCourrier.isSuisse());
			assertNull(adresseCourrier.getSalutations());
			assertNull(adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Nestlé Suisse S.A.", adresseDomicile.getLigne1());
			assertEquals("Usine d'Orbe", adresseDomicile.getLigne2());
			assertEquals("Entre-Deux-Villes", adresseDomicile.getLigne3());
			assertEquals("1800 Vevey", adresseDomicile.getLigne4());
			assertNull(adresseDomicile.getLigne5());
			assertNull(adresseDomicile.getLigne6());
			assertTrue(adresseDomicile.isSuisse());
			assertNull(adresseDomicile.getSalutations());
			assertNull(adresseDomicile.getFormuleAppel());

			assertAdressesEquals(adresseCourrier, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Nestlé Suisse S.A.", nomCourrier.get(0));
			assertEquals("Usine d'Orbe", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiDebiteurPrestationImposableAvecAdresseCourrierSurEntreprise() throws Exception {

		serviceInfra.setUp(new MockServiceInfrastructureService() {
			@Override
			protected void init() {
				pays.add(MockPays.Suisse);
				cantons.add(MockCanton.Vaud);
				communesVaud.add(MockCommune.Orbe);
				localites.add(MockLocalite.Orbe);
			}
		});

		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				addPM(MockPersonneMorale.NestleSuisse);
			}
		});

		// Crée un habitant et un débiteur associé
		final long noDebiteur = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
				{
					debiteur.setComplementNom("Usine d'Orbe");
					AdresseSuisse adresse = new AdresseSuisse();
					adresse.setRue("La plaine");
					adresse.setNumeroOrdrePoste(MockLocalite.Orbe.getNoOrdre());
					adresse.setDateDebut(RegDate.get(1950, 1, 1));
					adresse.setUsage(TypeAdresseTiers.COURRIER);
					debiteur.addAdresseTiers(adresse);
				}
				debiteur = (DebiteurPrestationImposable) tiersDAO.save(debiteur);
				long noDebiteur = debiteur.getNumero();

				Entreprise nestleSuisse = new Entreprise();
				nestleSuisse.setNumero(270123L);
				nestleSuisse.setNumeroEntreprise(MockPersonneMorale.NestleSuisse.getNumeroEntreprise());
				nestleSuisse = (Entreprise) tiersDAO.save(nestleSuisse);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nestleSuisse, debiteur);
				tiersDAO.getHibernateTemplate().merge(contact);

				return noDebiteur;
			}
		});

		{
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(noDebiteur);
			assertNotNull(debiteur);

			AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresseCourrier);
			assertEquals("Nestlé Suisse S.A.", adresseCourrier.getLigne1());
			assertEquals("Usine d'Orbe", adresseCourrier.getLigne2());
			assertEquals("La plaine", adresseCourrier.getLigne3());
			assertEquals("1350 Orbe", adresseCourrier.getLigne4());
			assertNull(adresseCourrier.getLigne5());
			assertTrue(adresseCourrier.isSuisse());
			assertNull(adresseCourrier.getSalutations());
			assertNull(adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Nestlé Suisse S.A.", adresseDomicile.getLigne1());
			assertEquals("Usine d'Orbe", adresseDomicile.getLigne2());
			assertEquals("Entre-Deux-Villes", adresseDomicile.getLigne3());
			assertEquals("1800 Vevey", adresseDomicile.getLigne4());
			assertNull(adresseDomicile.getLigne5());
			assertTrue(adresseDomicile.isSuisse());
			assertNull(adresseDomicile.getSalutations());
			assertNull(adresseDomicile.getFormuleAppel());

			AdresseEnvoiDetaillee adresseRepresentation = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false);
			assertNotNull(adresseRepresentation);
			assertEquals("Nestlé Suisse S.A.", adresseRepresentation.getLigne1());
			assertEquals("Usine d'Orbe", adresseRepresentation.getLigne2());
			assertEquals("Finance et Audit", adresseRepresentation.getLigne3());
			assertEquals("pa Myriam Steiner / CP 352", adresseRepresentation.getLigne4());
			assertEquals("1800 Vevey", adresseRepresentation.getLigne5());
			assertNull(adresseRepresentation.getLigne6());
			assertTrue(adresseRepresentation.isSuisse());
			assertNull(adresseRepresentation.getSalutations());
			assertNull(adresseRepresentation.getFormuleAppel());

			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Nestlé Suisse S.A.", nomCourrier.get(0));
			assertEquals("Usine d'Orbe", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiCollectiviteAdministrative() throws Exception {

		final int noCollectivite = 1;

		serviceInfra.setUp(new MockServiceInfrastructureService() {
			@Override
			protected void init() {
				// Pays
				pays.add(MockPays.Suisse);

				// Cantons
				cantons.add(MockCanton.Vaud);

				// Communes
				communesVaud.add(MockCommune.Lausanne);
				communesVaud.add(MockCommune.Cossonay);

				// Localités
				localites.add(MockLocalite.Lausanne);
				localites.add(MockLocalite.CossonayVille);

				// Rues
				rues.add(MockRue.CossonayVille.CheminDeRiondmorcel);
				rues.add(MockRue.Lausanne.AvenueDeBeaulieu);

				MockCollectiviteAdministrative collectivite = new MockCollectiviteAdministrative();
				collectivite.setNoColAdm(noCollectivite);
				collectivite.setNomCourt("Office Cantonal de la Sieste");
				collectivite.setNomComplet1("Office Cantonal de la Sieste et");
				collectivite.setNomComplet2("du Grand Repos");
				MockAdresse adresse = new MockAdresse();
				adresse.setNumero("4ter");
				adresse.setRue(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier());
				adresse.setNumeroPostal("1000");
				adresse.setLocalite("Lausanne");
				adresse.setDateDebutValidite(RegDate.get(2000, 1, 1));
				adresse.setTypeAdresse(TypeAdresseCivil.COURRIER);
				adresse.setCommuneAdresse(MockCommune.Lausanne);
				adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
				adresse.setNumeroOrdrePostal(MockLocalite.Lausanne.getNoOrdre());
				collectivite.setAdresse(adresse);
				collectivitesAdministrative.add(collectivite);
			}
		});

		// Crée un habitant et un débiteur associé
		final long noContribuable;
		{
			CollectiviteAdministrative collectivite = new CollectiviteAdministrative();
			collectivite.setNumeroCollectiviteAdministrative( noCollectivite);
			collectivite = (CollectiviteAdministrative) tiersDAO.save(collectivite);
			noContribuable = collectivite.getNumero();
		}

		{
			CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiersService.getTiers(noContribuable);
			assertNotNull(collectivite);

			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(collectivite, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Office Cantonal de la Sieste et", adresse.getLigne1());
			assertEquals("du Grand Repos", adresse.getLigne2());
			assertEquals("Av de Beaulieu 4ter", adresse.getLigne3());
			assertEquals("1000 Lausanne", adresse.getLigne4());
			assertNull(adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertNull(adresse.getSalutations());
			assertNull(adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(collectivite, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(collectivite, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(collectivite, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(collectivite, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Office Cantonal de la Sieste et", nomCourrier.get(0));
			assertEquals("du Grand Repos", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiPersonnePhysiqueSousTutelle() throws Exception {
		final long noPupille = 1;
		final long noTuteur = 2;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noPupille, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
							null, RegDate.get(1980, 1, 1), null);
					adresse.setNumero("3bis");
				}

				MockIndividu paul = addIndividu(noTuteur, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
							RegDate.get(1980, 1, 1), null);
					adresse.setNumero("14");
				}
			}
		});

		// Crée un le pupille et son tuteur
		final long noCtbPupille = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique pupille = new PersonnePhysique(true);
				pupille.setNumeroIndividu(noPupille);

				pupille = (PersonnePhysique) tiersDAO.save(pupille);
				long noCtbPupille = pupille.getNumero();

				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				RapportEntreTiers tutelle = new Tutelle();
				tutelle.setDateDebut(RegDate.get(2001, 3, 1));
				tutelle.setSujet(pupille);
				tutelle.setObjet(tuteur);

				tiersDAO.save(tutelle);
				return noCtbPupille;
			}
		});

		{
			PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(noCtbPupille);
			assertNotNull(habitant);

			AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresseCourrier);
			assertEquals("Monsieur", adresseCourrier.getLigne1());
			assertEquals("Pierre Dupont", adresseCourrier.getLigne2());
			assertEquals("p.a. Paul Durand", adresseCourrier.getLigne3());
			assertEquals("Avenue du Funiculaire 14", adresseCourrier.getLigne4());
			assertEquals("1304 Cossonay-Ville", adresseCourrier.getLigne5());
			assertNull(adresseCourrier.getLigne6());
			assertTrue(adresseCourrier.isSuisse());
			assertEquals("Monsieur", adresseCourrier.getSalutations());
			assertEquals("Monsieur", adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Monsieur", adresseDomicile.getLigne1());
			assertEquals("Pierre Dupont", adresseDomicile.getLigne2());
			assertEquals("Av de Beaulieu 3bis", adresseDomicile.getLigne3());
			assertEquals("1000 Lausanne", adresseDomicile.getLigne4());
			assertNull(adresseDomicile.getLigne5());
			assertNull(adresseDomicile.getLigne6());
			assertTrue(adresseDomicile.isSuisse());
			assertEquals("Monsieur", adresseDomicile.getSalutations());
			assertEquals("Monsieur", adresseDomicile.getFormuleAppel());
			
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(habitant, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(habitant, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
		}
	}

	@Test
	public void testGetAdresseEnvoiCoupleAvecPrincipalSousTutelle() throws Exception {
		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noTuteur = 5;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu marie = addIndividu(noIndividuConjoint, RegDate.get(1959, 3, 14), "Dupont", "Marie", false);

				marieIndividus(pierre, marie, RegDate.get(1980, 1, 1));

				// adresses courriers
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("23");

				MockIndividu paul = addIndividu(noTuteur, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
							RegDate.get(1980, 1, 1), null);
					adresse.setNumero("14");
				}
			}
		});

		// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
		final long noMenageCommun = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// ménage
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, RegDate.get(2004, 7, 14), null);
				principal = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				long noMenageCommun = menage.getNumero();

				// tuteur
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// tutelle
				RapportEntreTiers tutelle = new Tutelle();
				tutelle.setDateDebut(RegDate.get(2001, 3, 1));
				tutelle.setSujet(principal);
				tutelle.setObjet(tuteur);

				tiersDAO.save(tutelle);
				return noMenageCommun;
			}
		});

		{
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);
			assertNotNull(menage);

			// L'adresse courrier du couple correspondant à celle du conjoint, car celui-ci n'est pas sous tutelle
			AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresseCourrier);
			assertEquals("Monsieur et Madame", adresseCourrier.getLigne1());
			assertEquals("Pierre Dupont", adresseCourrier.getLigne2());
			assertEquals("Marie Dupont", adresseCourrier.getLigne3());
			assertEquals("Av de Marcelin 23", adresseCourrier.getLigne4());
			assertEquals("1000 Lausanne", adresseCourrier.getLigne5());
			assertNull(adresseCourrier.getLigne6());
			assertTrue(adresseCourrier.isSuisse());
			assertEquals("Monsieur et Madame", adresseCourrier.getSalutations());
			assertEquals("Monsieur et Madame", adresseCourrier.getFormuleAppel());

			// L'adresse de domicile est toujours (vraiment ?) celle du principal
			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Monsieur et Madame", adresseDomicile.getLigne1());
			assertEquals("Pierre Dupont", adresseDomicile.getLigne2());
			assertEquals("Marie Dupont", adresseDomicile.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresseDomicile.getLigne4());
			assertEquals("1000 Lausanne", adresseDomicile.getLigne5());
			assertNull(adresseDomicile.getLigne6());
			assertTrue(adresseDomicile.isSuisse());
			assertEquals("Monsieur et Madame", adresseDomicile.getSalutations());
			assertEquals("Monsieur et Madame", adresseDomicile.getFormuleAppel());

			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));
			
			List<String> nomCourrier = adresseService.getNomCourrier(menage, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Marie Dupont", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiCoupleAvecConjointSousTutelle() throws Exception {
		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noTuteur = 5;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu marie = addIndividu(noIndividuConjoint, RegDate.get(1959, 3, 14), "Dupont", "Marie", false);

				marieIndividus(pierre, marie, RegDate.get(1980, 1, 1));

				// adresses courriers
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("23");

				MockIndividu paul = addIndividu(noTuteur, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
							RegDate.get(1980, 1, 1), null);
					adresse.setNumero("14");
				}
			}
		});

		// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
		final long noMenageCommun = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// ménage
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, RegDate.get(2004, 7, 14), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				conjoint = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				long noMenageCommun = menage.getNumero();

				// tuteur
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(noTuteur);
				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);

				// tutelle
				RapportEntreTiers tutelle = new Tutelle();
				tutelle.setDateDebut(RegDate.get(2001, 3, 1));
				tutelle.setSujet(conjoint);
				tutelle.setObjet(tuteur);

				tiersDAO.save(tutelle);
				return noMenageCommun;
			}
		});

		{
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);
			assertNotNull(menage);

			// L'adresse courrier du couple correspondant à celle du principal, car la tutelle du conjoint est ignorée dans ce cas
			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresse);
			assertEquals("Monsieur et Madame", adresse.getLigne1());
			assertEquals("Pierre Dupont", adresse.getLigne2());
			assertEquals("Marie Dupont", adresse.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne4());
			assertEquals("1000 Lausanne", adresse.getLigne5());
			assertNull(adresse.getLigne6());
			assertTrue(adresse.isSuisse());
			assertEquals("Monsieur et Madame", adresse.getSalutations());
			assertEquals("Monsieur et Madame", adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(menage, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Marie Dupont", nomCourrier.get(1));
		}
	}

	@Test
	public void testGetAdresseEnvoiCouplePrincipalEtConjointSousTutelle() throws Exception {
		final long noIndividuPrincipal = 2;
		final long noIndividuConjoint = 4;
		final long noTuteurPrincipal = 5;
		final long noTuteurConjoint = 7;

		/*
		 * Crée les données du mock service civil
		 */
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pierre = addIndividu(noIndividuPrincipal, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				MockIndividu marie = addIndividu(noIndividuConjoint, RegDate.get(1959, 3, 14), "Dupont", "Marie", false);

				marieIndividus(pierre, marie, RegDate.get(1980, 1, 1));

				// adresses courriers
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("23");

				MockIndividu paul = addIndividu(noTuteurPrincipal, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
							RegDate.get(1980, 1, 1), null);
					adresse.setNumero("14");
				}

				MockIndividu jean = addIndividu(noTuteurConjoint, RegDate.get(1953, 11, 2), "Dupneu", "Jean", true);
				{
					// adresses courriers
					addAdresse(jean, TypeAdresseCivil.COURRIER, MockRue.LesClees.ChampDuRaffour, null,
							RegDate.get(1980, 1, 1), null);
				}
			}
		});

		// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
		final long noMenageCommun = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// ménage
				PersonnePhysique principal = new PersonnePhysique(true);
				principal.setNumeroIndividu(noIndividuPrincipal);
				PersonnePhysique conjoint = new PersonnePhysique(true);
				conjoint.setNumeroIndividu(noIndividuConjoint);
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, principal, RegDate.get(2004, 7, 14), null);
				principal = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				conjoint = (PersonnePhysique) tiersDAO.get(rapport.getSujetId());
				long noMenageCommun = menage.getNumero();

				// tuteur du principal
				PersonnePhysique tuteurPrincipal = new PersonnePhysique(true);
				tuteurPrincipal.setNumeroIndividu(noTuteurPrincipal);
				tuteurPrincipal = (PersonnePhysique) tiersDAO.save(tuteurPrincipal);

				// tutelle du principal
				RapportEntreTiers tutellePrincipal = new Tutelle();
				tutellePrincipal.setDateDebut(RegDate.get(2001, 3, 1));
				tutellePrincipal.setSujet(principal);
				tutellePrincipal.setObjet(tuteurPrincipal);

				tiersDAO.save(tutellePrincipal);

				// tuteur du conjoint
				PersonnePhysique tuteurConjoint = new PersonnePhysique(true);
				tuteurConjoint.setNumeroIndividu(noTuteurConjoint);
				tuteurConjoint = (PersonnePhysique) tiersDAO.save(tuteurConjoint);

				// tutelle du conjoint
				RapportEntreTiers tutelleConjoint = new Tutelle();
				tutelleConjoint.setDateDebut(RegDate.get(2001, 3, 1));
				tutelleConjoint.setSujet(conjoint);
				tutelleConjoint.setObjet(tuteurConjoint);

				tiersDAO.save(tutelleConjoint);
				return noMenageCommun;
			}
		});

		{
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(noMenageCommun);
			assertNotNull(menage);

			// L'adresse courrier du couple correspondant à celle du principal, car la tutelle du conjoint est ignorée dans ce cas
			AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, false);
			assertNotNull(adresseCourrier);
			assertEquals("Monsieur et Madame", adresseCourrier.getLigne1());
			assertEquals("Pierre Dupont", adresseCourrier.getLigne2());
			assertEquals("Marie Dupont", adresseCourrier.getLigne3());
			assertEquals("p.a. Paul Durand", adresseCourrier.getLigne4());
			assertEquals("Avenue du Funiculaire 14", adresseCourrier.getLigne5());
			assertEquals("1304 Cossonay-Ville", adresseCourrier.getLigne6());
			assertTrue(adresseCourrier.isSuisse());
			assertEquals("Monsieur et Madame", adresseCourrier.getSalutations());
			assertEquals("Monsieur et Madame", adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Monsieur et Madame", adresseDomicile.getLigne1());
			assertEquals("Pierre Dupont", adresseDomicile.getLigne2());
			assertEquals("Marie Dupont", adresseDomicile.getLigne3());
			assertEquals("Av de Beaulieu 3bis", adresseDomicile.getLigne4());
			assertEquals("1000 Lausanne", adresseDomicile.getLigne5());
			assertNull(adresseDomicile.getLigne6());
			assertTrue(adresseDomicile.isSuisse());
			assertEquals("Monsieur et Madame", adresseDomicile.getSalutations());
			assertEquals("Monsieur et Madame", adresseDomicile.getFormuleAppel());

			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(menage, null, false);
			assertNotNull(nomCourrier);
			assertEquals(2, nomCourrier.size());
			assertEquals("Pierre Dupont", nomCourrier.get(0));
			assertEquals("Marie Dupont", nomCourrier.get(1));
		}
	}

	/**
	 * [UNIREG-2915] Cas du contribuable n° 808'172'14 qui provoquait un assert dans le calcul du représentant (à cause du conjoint décédé)
	 */
	@Test
	public void testGetAdresseEnvoiCoupleConjointSousTutelleEtDecede() throws Exception {

		final long noIndPrincipal = 615125;
		final long noIndConjoint = 622948;
		final long noIndCurateur = 203256;

		final RegDate dateMariage = date(1984, 7, 1);
		final RegDate dateDecesConjoint = date(2009, 11, 26);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indPrincipal = addIndividu(noIndPrincipal, date(1919, 11, 13), "Rochat", "Maurice", true);
				addAdresse(indPrincipal, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDesBergieres, null, null, date(2010, 1, 31));
				addAdresse(indPrincipal, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDesBergieres, null, null, date(2010, 1, 31));
				addAdresse(indPrincipal, TypeAdresseCivil.PRINCIPALE, MockRue.Prilly.RueDesMetiers, null, date(2010, 2, 1), null);
				addAdresse(indPrincipal, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2010, 2, 1), null);

				MockIndividu indConjoint = addIndividu(noIndConjoint, date(1930, 11, 4), "Rochat", "Odette", false);
				indConjoint.setDateDeces(dateDecesConjoint);
				addAdresse(indConjoint, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDesBergieres, null, date(2010, 1, 31), null);
				addAdresse(indConjoint, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDesBergieres, null, date(2010, 1, 31), null);

				MockIndividu indCurateur = addIndividu(noIndCurateur, date(1948, 4, 27), "Rochat", "Jean-Pierre", true);
				addAdresse(indCurateur, TypeAdresseCivil.PRINCIPALE, MockRue.Prilly.CheminDeLaPossession, null, date(1973, 7, 21), null);
				addAdresse(indCurateur, TypeAdresseCivil.COURRIER, MockRue.Prilly.CheminDeLaPossession, null, date(1973, 7, 21), null);
			}
		});


		final long idPrincipal = 10407396L;
		final long idConjoint = 10407397L;
		final long idCurateur = 10169138L;
		final long idMenage = 80817214L;

		// Crée un ménage composé de deux habitants sans adresse fiscale surchargée
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique principal = addHabitant(idPrincipal, noIndPrincipal);
				final PersonnePhysique conjoint = addHabitant(idConjoint, noIndConjoint);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(idMenage, principal, conjoint, dateMariage, dateDecesConjoint);

				final MenageCommun menage = ensemble.getMenage();
				final AdresseSuisse adresse = addAdresseSuisse(menage, TypeAdresseTiers.COURRIER, date(2010, 7, 1), null, MockRue.Prilly.CheminDeLaPossession);
				adresse.setComplement("p.a. Jean-Pierre Rochat");

				final PersonnePhysique curateur = addHabitant(idCurateur, noIndCurateur);
				addCuratelle(conjoint, curateur, date(2009, 4, 3), dateDecesConjoint);
				addCuratelle(principal, curateur, date(2009, 4, 3), null);

				return null;
			}
		});

		final MenageCommun menage = (MenageCommun) tiersService.getTiers(idMenage);

		// Vérification des adresses
		AdresseEnvoi adresse = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
		assertNotNull(adresse);
		assertEquals("Aux héritiers de", adresse.getLigne1());
		assertEquals("Maurice Rochat", adresse.getLigne2());
		assertEquals("Odette Rochat, défunte", adresse.getLigne3());
		assertEquals("p.a. Jean-Pierre Rochat", adresse.getLigne4());
		assertEquals("Chemin de la Possession", adresse.getLigne5());
		assertEquals("1008 Prilly", adresse.getLigne6());
	}

	// [UNIREG-749] Ajout d'un suffixe 'défunt' en cas de décès
	@Test
	public void testGetAdresseEnvoiCoupleEnCasDeces() throws Exception {

		final RegDate dateDeces = date(2002, 2, 2);
		final RegDate aujourdhui = RegDate.get();

		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965,3,23), Sexe.MASCULIN);
		PersonnePhysique joelle = addNonHabitant("Joëlle", "Duchêne", date(1968,12,3), Sexe.FEMININ);
		MenageCommun menage = new MenageCommun();

		RapportEntreTiers rapportPaul = new AppartenanceMenage(date(1988,1,1), null, paul, menage);
		menage.addRapportObjet(rapportPaul);
		paul.addRapportSujet(rapportPaul);

		RapportEntreTiers rapportJoelle = new AppartenanceMenage(date(1988,1,1), null, joelle, menage);
		menage.addRapportObjet(rapportJoelle);
		joelle.addRapportSujet(rapportJoelle);

		AdresseSuisse adresseSuisse = new AdresseSuisse();
		adresseSuisse.setDateDebut(date(1988,1,1));
		adresseSuisse.setDateFin(null);
		adresseSuisse.setUsage(TypeAdresseTiers.COURRIER);
		adresseSuisse.setNumeroRue(MockRue.CossonayVille.AvenueDuFuniculaire.getNoRue());
		adresseSuisse.setNumeroMaison("14");
		adresseSuisse.setNumeroOrdrePoste(MockRue.CossonayVille.AvenueDuFuniculaire.getLocalite().getNPA());
		paul.addAdresseTiers(adresseSuisse);

		// Adresse telle que non-décédés
		AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(menage, aujourdhui, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresse);
		assertEquals("Monsieur et Madame", adresse.getLigne1());
		assertEquals("Paul Duchêne", adresse.getLigne2());
		assertEquals("Joëlle Duchêne", adresse.getLigne3());
		assertEquals("Avenue du Funiculaire 14", adresse.getLigne4());
		assertEquals("1304 Cossonay-Ville", adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertTrue(adresse.isSuisse());
		assertEquals("Monsieur et Madame", adresse.getSalutations());
		assertEquals("Monsieur et Madame", adresse.getFormuleAppel());

		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));

		// Décès de Paul
		paul.setDateDeces(dateDeces);
		rapportPaul.setDateFin(dateDeces);
		rapportJoelle.setDateFin(dateDeces);

		adresse = adresseService.getAdresseEnvoi(menage, aujourdhui, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresse);
		assertEquals("Aux héritiers de", adresse.getLigne1());
		assertEquals("Paul Duchêne, défunt", adresse.getLigne2());
		assertEquals("Joëlle Duchêne", adresse.getLigne3());
		assertEquals("Avenue du Funiculaire 14", adresse.getLigne4());
		assertEquals("1304 Cossonay-Ville", adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertTrue(adresse.isSuisse());
		assertEquals("Aux héritiers de", adresse.getSalutations());
		assertEquals("Madame, Monsieur", adresse.getFormuleAppel()); // [UNIREG-1398]

		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));

		// Décès de Joëlle
		paul.setDateDeces(null);
		joelle.setDateDeces(dateDeces);
		rapportPaul.setDateFin(dateDeces);
		rapportJoelle.setDateFin(dateDeces);

		adresse = adresseService.getAdresseEnvoi(menage, aujourdhui, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresse);
		assertEquals("Aux héritiers de", adresse.getLigne1());
		assertEquals("Paul Duchêne", adresse.getLigne2());
		assertEquals("Joëlle Duchêne, défunte", adresse.getLigne3());
		assertEquals("Avenue du Funiculaire 14", adresse.getLigne4());
		assertEquals("1304 Cossonay-Ville", adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertTrue(adresse.isSuisse());
		assertEquals("Aux héritiers de", adresse.getSalutations());
		assertEquals("Madame, Monsieur", adresse.getFormuleAppel()); // [UNIREG-1398]

		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));

		// Décès des deux
		paul.setDateDeces(dateDeces);
		joelle.setDateDeces(dateDeces);
		rapportPaul.setDateFin(dateDeces);
		rapportJoelle.setDateFin(dateDeces);

		adresse = adresseService.getAdresseEnvoi(menage, aujourdhui, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresse);
		assertEquals("Aux héritiers de", adresse.getLigne1());
		assertEquals("Paul Duchêne, défunt", adresse.getLigne2());
		assertEquals("Joëlle Duchêne, défunte", adresse.getLigne3());
		assertEquals("Avenue du Funiculaire 14", adresse.getLigne4());
		assertEquals("1304 Cossonay-Ville", adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertTrue(adresse.isSuisse());
		assertEquals("Aux héritiers de", adresse.getSalutations());
		assertEquals("Madame, Monsieur", adresse.getFormuleAppel()); // [UNIREG-1398]

		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.DOMICILE, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.POURSUITE, false));
	}

	@Test
	public void testGetAdresseEnvoiEntrangere() throws Exception {

		final RegDate aujourdhui = RegDate.get();

		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965,3,23), Sexe.MASCULIN);

		AdresseEtrangere adresseEtrangere = new AdresseEtrangere();
		adresseEtrangere.setDateDebut(date(1988,1,1));
		adresseEtrangere.setDateFin(null);
		adresseEtrangere.setUsage(TypeAdresseTiers.COURRIER);
		adresseEtrangere.setRue("Rue Centrale");
		adresseEtrangere.setNumeroMaison("23b");
		adresseEtrangere.setNumeroPostalLocalite("123456 Montargis");
		adresseEtrangere.setNumeroOfsPays(MockPays.France.getNoOFS());
		paul.addAdresseTiers(adresseEtrangere);

		// Adresse d'envoi hors-Suisse
		AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(paul, aujourdhui, TypeAdresseFiscale.COURRIER, false);
		assertNotNull(adresse);
		assertEquals("Monsieur", adresse.getLigne1());
		assertEquals("Paul Duchêne", adresse.getLigne2());
		assertEquals("Rue Centrale 23b", adresse.getLigne3());
		assertEquals("123456 Montargis", adresse.getLigne4());
		assertEquals("France", adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertFalse(adresse.isSuisse());
		assertEquals("Monsieur", adresse.getSalutations());
		assertEquals("Monsieur", adresse.getFormuleAppel());

		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(paul, null, TypeAdresseFiscale.DOMICILE, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(paul, null, TypeAdresseFiscale.REPRESENTATION, false));
		assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(paul, null, TypeAdresseFiscale.POURSUITE, false));
	}

	@Test
	public void testAdresseEnvoiCurateurDeMadameHabitanteAvecMonsieurNonHabitant() throws Exception {

		// test créé pour le cas jira UNIREG-1954

		final long noIndividuMadame = 12345L;
		final long noIndividuCurateurMadame = 12346L;
		final long noIndividuTuteurMonsieur = 3245L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// voilà madame
				final MockIndividu albertine = addIndividu(noIndividuMadame, date(1954, 5, 2), "Pittet", "Albertine", false);
				addAdresse(albertine, TypeAdresseCivil.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null, date(1980, 1, 1), null);
				addAdresse(albertine, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, date(1980, 1, 1), null);

				// c'est le curateur (de madame)
				final MockIndividu pierre = addIndividu(noIndividuCurateurMadame, date(1953, 11, 2), "Dupont", "Pierre", true);
				addAdresse(pierre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1980, 1, 1), null);

				// et le tuteur (de monsieur)
				final MockIndividu nicolas = addIndividu(noIndividuTuteurMonsieur, date(1940, 1, 15), "Ricola", "Nicolas", true);
				addAdresse(nicolas, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null, date(1980, 1, 1), null);
				addAdresse(nicolas, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1980, 1, 1), null);
			}
		});

		final PersonnePhysique monsieur = addNonHabitant("Achille", "Talon", date(1963,11,7), Sexe.MASCULIN);
		final PersonnePhysique madame = addHabitant(noIndividuMadame);
		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(2000, 1, 1), null);
		final MenageCommun mc = ensemble.getMenage();

		final PersonnePhysique tuteurMonsieur = addHabitant(noIndividuTuteurMonsieur);
		final Tutelle tutelle = new Tutelle(date(2000,1,1), null, monsieur, tuteurMonsieur, null);
		monsieur.addRapportSujet(tutelle);
		tuteurMonsieur.addRapportObjet(tutelle);

		final PersonnePhysique curateurMadame = addHabitant(noIndividuCurateurMadame);
		final Curatelle curatelle = new Curatelle(date(2000,1,1), null, madame, curateurMadame, null);
		madame.addRapportSujet(curatelle);
		curateurMadame.addRapportObjet(curatelle);

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(mc, null, TypeAdresseFiscale.COURRIER, true);
		assertNotNull(adresseEnvoi);
		assertEquals("Pierre Dupont est le curateur de Madame, seule habitante du couple", "p.a. Pierre Dupont", adresseEnvoi.getPourAdresse());
	}

	/**
	 * [UNIREG-1974] Vérifie que l'adresse de la fiduciaire Jal Holding utilise bien les trois lignes de la raison sociale et non pas la raison sociale abbrégée.
	 */
	@Test
	public void testGetAdresseEnvoiJalHolding() throws Exception {

		servicePM.setUp(new DefaultMockServicePM());

		final Entreprise jal = new Entreprise();
		jal.setNumero(MockPersonneMorale.JalHolding.getNumeroEntreprise());
		jal.setNumeroEntreprise(MockPersonneMorale.JalHolding.getNumeroEntreprise());

		final AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(jal, null, TypeAdresseFiscale.COURRIER, true);
		assertNotNull(adresseCourrier);
		assertEquals("Jal holding S.A.", adresseCourrier.getLigne1()); // <-- raison sociale ligne 1
		assertEquals("en liquidation", adresseCourrier.getLigne2()); // <-- raison sociale ligne 3 (la ligne 2 est vide)
		assertEquals("pa Fidu. Commerce & Industrie", adresseCourrier.getLigne3());
		assertEquals("Avenue de la Gare 10", adresseCourrier.getLigne4());
		assertEquals("1003 Lausanne", adresseCourrier.getLigne5());
		assertNull(adresseCourrier.getLigne6());
		assertTrue(adresseCourrier.isSuisse());
		assertNull(adresseCourrier.getSalutations());
		assertNull(adresseCourrier.getFormuleAppel());

		final AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(jal, null, TypeAdresseFiscale.DOMICILE, true);
		assertNotNull(adresseDomicile);
		assertEquals("Jal holding S.A.", adresseDomicile.getLigne1()); // <-- raison sociale ligne 1
		assertEquals("en liquidation", adresseDomicile.getLigne2()); // <-- raison sociale ligne 3 (la ligne 2 est vide)
		assertEquals("Fid.Commerce & Industrie S.A.", adresseDomicile.getLigne3());
		assertEquals("Chemin Messidor 5", adresseDomicile.getLigne4());
		assertEquals("1006 Lausanne", adresseDomicile.getLigne5());
		assertNull(adresseDomicile.getLigne6());
		assertTrue(adresseDomicile.isSuisse());
		assertNull(adresseDomicile.getSalutations());
		assertNull(adresseDomicile.getFormuleAppel());
	}

	/**
	 * [UNIREG-1974] Vérifie que l'adresse de la PM Evian-Russie tient bien sur 6 lignes et que le complément d'adresse est ignoré
	 */
	@Test
	public void testGetAdresseEnvoiEvianRussie() throws Exception {

		servicePM.setUp(new DefaultMockServicePM());

		final Entreprise evian = new Entreprise();
		evian.setNumero(MockPersonneMorale.EvianRussie.getNumeroEntreprise());
		evian.setNumeroEntreprise(MockPersonneMorale.EvianRussie.getNumeroEntreprise());

		final AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(evian, null, TypeAdresseFiscale.COURRIER, true);
		assertNotNull(adresseCourrier);
		assertEquals("Distributor (Evian Water)", adresseCourrier.getLigne1()); // <-- raison sociale ligne 1
		assertEquals("LLC PepsiCo Holdings", adresseCourrier.getLigne2()); // <-- raison sociale ligne 2
		assertEquals("Free Economic Zone Sherrizone", adresseCourrier.getLigne3()); // <-- raison sociale ligne 3

		// [UNIREG-1974] le complément est ignoré pour que l'adresse tienne sur 6 lignes
		// assertEquals("p.a. Aleksey Fyodorovich Karamazov", adresseCourrier.getLigneXXX());

		assertEquals("Solnechnogorsk Dist.", adresseCourrier.getLigne4()); // <-- rue
		assertEquals("141580 Moscow region", adresseCourrier.getLigne5()); // <-- npa + lieu
		assertEquals("Russie", adresseCourrier.getLigne6()); // <-- pays
		assertFalse(adresseCourrier.isSuisse());
		assertNull(adresseCourrier.getSalutations());
		assertNull(adresseCourrier.getFormuleAppel());
	}

	private static void assertAdressesEquals(AdresseEnvoiDetaillee expected, AdresseEnvoiDetaillee actual) {
		assertNotNull(actual);
		assertEquals(expected.getLigne1(), actual.getLigne1());
		assertEquals(expected.getLigne2(), actual.getLigne2());
		assertEquals(expected.getLigne3(), actual.getLigne3());
		assertEquals(expected.getLigne4(), actual.getLigne4());
		assertEquals(expected.getLigne5(), actual.getLigne5());
		assertEquals(expected.getLigne6(), actual.getLigne6());
		assertEquals(expected.isSuisse(), actual.isSuisse());
		assertEquals(expected.getSalutations(), actual.getSalutations());
		assertEquals(expected.getFormuleAppel(), actual.getFormuleAppel());
	}
}

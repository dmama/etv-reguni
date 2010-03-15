package ch.vd.uniregctb.adresse;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.*;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static org.junit.Assert.*;

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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
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
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
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
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				menage = (MenageCommun) rapport.getObjet();
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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
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
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				menage = (MenageCommun) rapport.getObjet();
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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
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
			assertEquals("Ma petite entreprise", adresse.getLigne1());
			assertEquals("Av de Beaulieu 3bis", adresse.getLigne2());
			assertEquals("1000 Lausanne", adresse.getLigne3());
			assertNull(adresse.getLigne4());
			assertTrue(adresse.isSuisse());
			assertNull(adresse.getSalutations());
			assertNull(adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Ma petite entreprise", nomCourrier.get(0));
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
				MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
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
			assertEquals("Ma petite entreprise", adresseCourrier.getLigne1());
			assertEquals("Chemin de Riondmorcel 2bis", adresseCourrier.getLigne2());
			assertEquals("1304 Cossonay-Ville", adresseCourrier.getLigne3());
			assertNull(adresseCourrier.getLigne4());
			assertTrue(adresseCourrier.isSuisse());
			assertNull(adresseCourrier.getSalutations());
			assertNull(adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Ma petite entreprise", adresseDomicile.getLigne1());
			assertEquals("Av de Beaulieu 3bis", adresseDomicile.getLigne2());
			assertEquals("1000 Lausanne", adresseDomicile.getLigne3());
			assertNull(adresseDomicile.getLigne4());
			assertTrue(adresseDomicile.isSuisse());
			assertNull(adresseDomicile.getSalutations());
			assertNull(adresseDomicile.getFormuleAppel());

			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Ma petite entreprise", nomCourrier.get(0));
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
				adresse.setTypeAdresse(EnumTypeAdresse.COURRIER);
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
			assertEquals("Service du Personnel", adresse.getLigne1());
			assertEquals("Route du Signal 8", adresse.getLigne2());
			assertEquals("1014 Lausanne", adresse.getLigne3());
			assertNull(adresse.getLigne4());
			assertTrue(adresse.isSuisse());
			assertNull(adresse.getSalutations());
			assertNull(adresse.getFormuleAppel());

			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.DOMICILE, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresse, adresseService.getAdresseEnvoi(tribunal, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(tribunal, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Service du Personnel", nomCourrier.get(0));
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
			assertEquals("Usine d'Orbe", adresseCourrier.getLigne1());
			assertEquals("Finance et Audit", adresseCourrier.getLigne2());
			assertEquals("pa Myriam Steiner / CP 352", adresseCourrier.getLigne3()); // il s'agit de la rue, cette adresse est très mal définie dans la base, en fait.
			assertEquals("1800 Vevey", adresseCourrier.getLigne4());
			assertNull(adresseCourrier.getLigne5());
			assertTrue(adresseCourrier.isSuisse());
			assertNull(adresseCourrier.getSalutations());
			assertNull(adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Usine d'Orbe", adresseDomicile.getLigne1());
			assertEquals("Entre-Deux-Villes", adresseDomicile.getLigne2());
			assertEquals("1800 Vevey", adresseDomicile.getLigne3());
			assertNull(adresseDomicile.getLigne4());
			assertNull(adresseDomicile.getLigne5());
			assertTrue(adresseDomicile.isSuisse());
			assertNull(adresseDomicile.getSalutations());
			assertNull(adresseDomicile.getFormuleAppel());

			assertAdressesEquals(adresseCourrier, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false));
			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Usine d'Orbe", nomCourrier.get(0));
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
			assertEquals("Usine d'Orbe", adresseCourrier.getLigne1());
			assertEquals("La plaine", adresseCourrier.getLigne2());
			assertEquals("1350 Orbe", adresseCourrier.getLigne3());
			assertNull(adresseCourrier.getLigne4());
			assertTrue(adresseCourrier.isSuisse());
			assertNull(adresseCourrier.getSalutations());
			assertNull(adresseCourrier.getFormuleAppel());

			AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.DOMICILE, false);
			assertNotNull(adresseDomicile);
			assertEquals("Usine d'Orbe", adresseDomicile.getLigne1());
			assertEquals("Entre-Deux-Villes", adresseDomicile.getLigne2());
			assertEquals("1800 Vevey", adresseDomicile.getLigne3());
			assertNull(adresseDomicile.getLigne4());
			assertTrue(adresseDomicile.isSuisse());
			assertNull(adresseDomicile.getSalutations());
			assertNull(adresseDomicile.getFormuleAppel());

			AdresseEnvoiDetaillee adresseRepresentation = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.REPRESENTATION, false);
			assertNotNull(adresseRepresentation);
			assertEquals("Usine d'Orbe", adresseRepresentation.getLigne1());
			assertEquals("Finance et Audit", adresseRepresentation.getLigne2());
			assertEquals("pa Myriam Steiner / CP 352", adresseRepresentation.getLigne3());
			assertEquals("1800 Vevey", adresseRepresentation.getLigne4());
			assertNull(adresseRepresentation.getLigne5());
			assertNull(adresseRepresentation.getLigne6());
			assertTrue(adresseRepresentation.isSuisse());
			assertNull(adresseRepresentation.getSalutations());
			assertNull(adresseRepresentation.getFormuleAppel());

			assertAdressesEquals(adresseDomicile, adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.POURSUITE, false));

			List<String> nomCourrier = adresseService.getNomCourrier(debiteur, null, false);
			assertNotNull(nomCourrier);
			assertEquals(1, nomCourrier.size());
			assertEquals("Usine d'Orbe", nomCourrier.get(0));
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
				adresse.setTypeAdresse(EnumTypeAdresse.COURRIER);
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
					MockAdresse adresse = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
							null, RegDate.get(1980, 1, 1), null);
					adresse.setNumero("3bis");
				}

				MockIndividu paul = addIndividu(noTuteur, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
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
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeMarcelin,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("23");

				MockIndividu paul = addIndividu(noTuteur, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
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
				principal = (PersonnePhysique) rapport.getSujet();
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				menage = (MenageCommun) rapport.getObjet();
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
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeMarcelin,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("23");

				MockIndividu paul = addIndividu(noTuteur, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
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
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				conjoint = (PersonnePhysique) rapport.getSujet();
				menage = (MenageCommun) rapport.getObjet();
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
				MockAdresse adressePierre = (MockAdresse) addAdresse(pierre, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu,
						null, RegDate.get(1980, 1, 1), null);
				adressePierre.setNumero("3bis");
				MockAdresse adresseMarie = (MockAdresse) addAdresse(marie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeMarcelin,
						null, RegDate.get(1980, 1, 1), null);
				adresseMarie.setNumero("23");

				MockIndividu paul = addIndividu(noTuteurPrincipal, RegDate.get(1953, 11, 2), "Durand", "Paul", true);
				{
					// adresses courriers
					MockAdresse adresse = (MockAdresse) addAdresse(paul, EnumTypeAdresse.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null,
							RegDate.get(1980, 1, 1), null);
					adresse.setNumero("14");
				}

				MockIndividu jean = addIndividu(noTuteurConjoint, RegDate.get(1953, 11, 2), "Dupneu", "Jean", true);
				{
					// adresses courriers
					addAdresse(jean, EnumTypeAdresse.COURRIER, MockRue.LesClees.ChampDuRaffour, null,
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
				principal = (PersonnePhysique) rapport.getSujet();
				menage = (MenageCommun) rapport.getObjet();
				rapport = tiersService.addTiersToCouple(menage, conjoint, RegDate.get(1980, 1, 1), null);
				conjoint = (PersonnePhysique) rapport.getSujet();
				menage = (MenageCommun) rapport.getObjet();
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

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(mc, null, TypeAdresseFiscale.COURRIER, true);
		assertNotNull(adresseEnvoi);
		assertEquals("Pierre Dupont est le curateur de Madame, seule habitante du couple", "p.a. Pierre Dupont", adresseEnvoi.getPourAdresse());
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

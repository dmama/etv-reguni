package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
@SuppressWarnings({"JavaDoc"})
public class TiersAdresseControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersAdresseController";

	private final static String DB_UNIT_FILE = "TiersAdresseControllerTest.xml";

	private final static String NUMERO_CTB_PARAMETER_NAME = "numero";

	private final static String ID_ADRESSE_PARAMETER_NAME = "idAdresse";

	private TiersAdresseController controller;
	private AdresseTiersDAO adresseTiersDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		controller = getBean(TiersAdresseController.class, CONTROLLER_NAME);
		adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				final MockIndividu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);

				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("GET");
		request.addParameter(NUMERO_CTB_PARAMETER_NAME, "6789");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowFormExistingAdresse() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("GET");
		request.addParameter(NUMERO_CTB_PARAMETER_NAME, "6789");
		request.addParameter(ID_ADRESSE_PARAMETER_NAME, "5");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitAdresseEtrangere() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("numCTB", "67895");
		request.addParameter("localiteNpa", "Paris");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.addParameter("dateDebut", "12.02.2002");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(1, tiers.getAdressesTiers().size());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitAdresseSuisse() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("numCTB", "67895");
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.addParameter("dateDebut", "12.02.2002");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(1, tiers.getAdressesTiers().size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitAdresseSuisseWithNoDateDebut() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numCTB", "67895");
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.setMethod("POST");

		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(0, tiers.getAdressesTiers().size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitAdresseEtrangereWithNoDateDebut() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numCTB", "67895");
		request.addParameter("localiteNpa", "Paris");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		Tiers tiers = tiersDAO.get((long) 67895);
		assertEquals(0, tiers.getAdressesTiers().size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitModifyAdresseSuisse() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numCTB", "6789");
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.addParameter("dateDebut", "23.08.2008");
		request.addParameter("id", "192");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		AdresseTiers adresseTiers = adresseTiersDAO.get((long) 192);
		AdresseSuisse addSuisse = (AdresseSuisse) adresseTiers;
		assertEquals(165, addSuisse.getNumeroOrdrePoste().intValue());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitModifyAdresseEtrangere() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.addParameter("numCTB", "6789");
		request.addParameter("localiteNpa", "Marseille");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.addParameter("dateDebut", "12.02.2006");
		request.addParameter("id", "5");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		AdresseTiers adresseTiers = adresseTiersDAO.get((long) 5);
		AdresseEtrangere addEtrangere = (AdresseEtrangere) adresseTiers;
		assertEquals("Marseille", addEtrangere.getNumeroPostalLocalite());
	}

	/**
	 * [UNIREG-3152] Vérifie que la reprise d'une adresse de type 'autre tiers' fonctionne correctement, et notamment que les ids tiers_id et autre_tiers_id ne sont pas les mêmes.
	 */
	@Test
	public void testRepriseAdresseAutreTiers() throws Exception {

		class Ids {
			long pupille;
			long tuteur;
		}
		final Ids ids = new Ids();

		// Crée un pupille et sont tuteur, sans autre lien
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pupille = addNonHabitant("Jean", "Pupille", date(1980, 1, 1), Sexe.MASCULIN);
				addAdresseSuisse(pupille, TypeAdresseTiers.DOMICILE, date(1980, 1, 1), null, MockRue.CossonayVille.AvenueDuFuniculaire);
				ids.pupille = pupille.getId();

				final PersonnePhysique tuteur = addNonHabitant("Jacques", "Tuteur", date(1980, 1, 1), Sexe.MASCULIN);
				addAdresseSuisse(tuteur, TypeAdresseTiers.DOMICILE, date(1980, 1, 1), null, MockRue.CossonayVille.CheminDeRiondmorcel);
				addTutelle(pupille, tuteur, null, date(2005, 1, 1), null);
				ids.tuteur = tuteur.getId();

				return null;
			}
		});

		// Demande la reprise de l'adresse de représentation du tuteur comme adresse de représentation du pupille.
		request.clearAttributes();
		request.addParameter("numero", String.valueOf(ids.pupille));
		request.addParameter("usage", TypeAdresseTiers.REPRESENTATION.name());
		request.addParameter("dateDebut", "01.01.2005");
		request.addParameter("mode", "reprise");
		request.addParameter("index", "0");
		request.setMethod("POST");
		controller.handleRequest(request, response);

		// On vérifie que l'adresse autre tiers a été créée correctement en base
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(ids.pupille);
				assertNotNull(pupille);

				final List<AdresseTiers> adresses = pupille.getAdressesTiersSorted();
				assertNotNull(adresses);
				assertEquals(2, adresses.size()); // l'adresse de domicile du pupille + l'adresse autre tiers

				final AdresseAutreTiers adresseAutreTiers = (AdresseAutreTiers) adresses.get(1);
				assertNotNull(adresseAutreTiers);
				assertEquals(date(2005, 1, 1), adresseAutreTiers.getDateDebut());
				assertNull(adresseAutreTiers.getDateFin());
				assertEquals(TypeAdresseTiers.REPRESENTATION, adresseAutreTiers.getUsage());
				assertEquals(TypeAdresseTiers.REPRESENTATION, adresseAutreTiers.getType());
				assertEquals(Long.valueOf(ids.pupille), adresseAutreTiers.getTiers().getId());
				assertEquals(Long.valueOf(ids.tuteur), adresseAutreTiers.getAutreTiersId());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-156] Vérifie que les mise-à-jour des adresses successorales s'effectue bien
	 */
	@Test
	public void testMiseAJourAdressesSuccessorales() throws Exception {

		class Ids {
			long conjoint;
			long principal;
			long menage;
		}
		final Ids ids = new Ids();

		// Création d'un couple avec un membre décédé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique jacques = addNonHabitant("Jacques", "Pignut", date(1932, 1, 1), Sexe.MASCULIN);
				jacques.setDateDeces(date(2008, 11, 4));
				addAdresseSuisse(jacques, TypeAdresseTiers.DOMICILE, date(1932, 1, 1), null, MockRue.CossonayVille.CheminDeRiondmorcel);
				ids.principal = jacques.getId();

				final PersonnePhysique jeanne = addNonHabitant("Jeanne", "Pignut", date(1945, 1, 1), Sexe.FEMININ);
				addAdresseSuisse(jeanne, TypeAdresseTiers.DOMICILE, date(1945, 1, 1), null, MockRue.CossonayVille.AvenueDuFuniculaire);
				ids.conjoint = jeanne.getId();

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jacques, jeanne, date(1961, 5, 1), date(2008, 11, 4));
				ids.menage = ensemble.getMenage().getNumero();

				return null;
			}
		});

		// Ajout d'une adresse courrier dites "successorale" sur le ménage
		request.clearAttributes();
		request.addParameter("numCTB", String.valueOf(ids.menage));
		request.addParameter("usage", TypeAdresseTiers.COURRIER.name());
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("dateDebut", "12.02.2010");

		// l'état successoral
		request.addParameter("etatSuccessoral.numeroPrincipalDecede", String.valueOf(ids.principal));
		request.addParameter("mettreAJourDecedes", "true");

		request.setMethod("POST");
		controller.handleRequest(request, response);

		// On vérifie que l'adresse saisie a été ajoutée à la fois sur le ménage et sur le principal décédé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(menage);

				final List<AdresseTiers> adressesMenage = menage.getAdressesTiersSorted();
				assertNotNull(adressesMenage);
				assertEquals(1, adressesMenage.size());

				final AdresseSuisse adresseSuccMenage = (AdresseSuisse) adressesMenage.get(0);
				assertNotNull(adresseSuccMenage);
				assertEquals(date(2010, 2, 12), adresseSuccMenage.getDateDebut());
				assertNull(adresseSuccMenage.getDateFin());
				assertEquals(TypeAdresseTiers.COURRIER, adresseSuccMenage.getUsage());
				assertEquals(Integer.valueOf(165), adresseSuccMenage.getNumeroOrdrePoste());

				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(ids.principal);
				assertNotNull(principal);

				final List<AdresseTiers> adressesDefunt = principal.getAdressesTiersSorted();
				assertNotNull(adressesDefunt);
				assertEquals(2, adressesDefunt.size());

				final AdresseSuisse adresseSuccDefunt = (AdresseSuisse) adressesDefunt.get(1);
				assertNotNull(adresseSuccDefunt);
				assertEquals(date(2010, 2, 12), adresseSuccDefunt.getDateDebut());
				assertNull(adresseSuccDefunt.getDateFin());
				assertEquals(TypeAdresseTiers.COURRIER, adresseSuccDefunt.getUsage());
				assertEquals(Integer.valueOf(165), adresseSuccDefunt.getNumeroOrdrePoste());
				return null;
			}
		});
	}
}

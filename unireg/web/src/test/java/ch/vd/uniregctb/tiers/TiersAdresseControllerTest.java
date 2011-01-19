package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
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
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);
		request.setMethod("GET");
		request.addParameter(NUMERO_CTB_PARAMETER_NAME, "6789");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	@Test
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
	@NotTransactional
	@Test
	public void testRepriseAdresseAutreTiers() throws Exception {

		class Ids {
			long pupille;
			long tuteur;
		}
		final Ids ids = new Ids();

		// Crée un pupille et sont tuteur, sans autre lien
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
}

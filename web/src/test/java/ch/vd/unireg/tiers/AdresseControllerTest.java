package ch.vd.unireg.tiers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseEtrangere;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseSupplementaire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test case du controlleur spring du meme nom.
 */
@SuppressWarnings({"JavaDoc"})
public class AdresseControllerTest extends WebTestSpring3 {

	private static final String NUMERO_CTB_PARAMETER_NAME = "numero";
	private static final String ID_ADRESSE_PARAMETER_NAME = "idAdresse";

	@Test
	public void testShowAddView() throws Exception {

		final int noIndividu = 282315;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addAdresseEtrangere(pp, TypeAdresseTiers.REPRESENTATION, date(2006, 2, 12), date(2006, 4, 12), "12 Avenue des Ternes", "75012 Paris", MockPays.France);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2008, 8, 23), null, MockRue.Lausanne.AvenueDesBergieres);
			return pp.getNumero();
		});

		request.setMethod("GET");
		request.setRequestURI("/adresses/adresse-add.do");
		request.addParameter(NUMERO_CTB_PARAMETER_NAME, String.valueOf(ppId));
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	public void testShowCloseView() throws Exception {

		final int noIndividu = 282315;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		final class Ids {
			long idTiers;
			long idAdresse;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addAdresseEtrangere(pp, TypeAdresseTiers.REPRESENTATION, date(2006, 2, 12), date(2006, 4, 12), "12 Avenue des Ternes", "75012 Paris", MockPays.France);
			final AdresseSuisse adresse = addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2008, 8, 23), null, MockRue.Lausanne.AvenueDesBergieres);
			adresse.setNumeroMaison("186b");

			final Ids ids1 = new Ids();
			ids1.idAdresse = adresse.getId();
			ids1.idTiers = pp.getNumero();
			return ids1;
		});

		request.setMethod("GET");
		request.setRequestURI("/adresses/adresse-close.do");
		request.addParameter(ID_ADRESSE_PARAMETER_NAME, String.valueOf(ids.idAdresse));
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);

		final AdresseView view = (AdresseView) model.get("view");
		assertNotNull(view);
		assertEquals((Long) ids.idAdresse, view.getId());
		assertEquals(date(2008, 8, 23), view.getDateDebut());
		assertNull(view.getDateFin());
		assertEquals(MockRue.Lausanne.AvenueDesBergieres.getDesignationCourrier(), view.getRue());
		assertEquals(MockRue.Lausanne.AvenueDesBergieres.getNoRue(), view.getNumeroRue());
		assertEquals("186b", view.getNumeroMaison());
		assertEquals("Lausanne", view.getLocaliteSuisse());
		assertEquals(TypeAdresseTiers.COURRIER, view.getUsage());
		assertEquals((Long) ids.idTiers, view.getNumCTB());
	}

	@Test
	public void testCloseAdresse() throws Exception {

		final int noIndividu = 282315;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		final class Ids {
			long idTiers;
			long idAdresse;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addAdresseEtrangere(pp, TypeAdresseTiers.REPRESENTATION, date(2006, 2, 12), date(2006, 4, 12), "12 Avenue des Ternes", "75012 Paris", MockPays.France);
			final AdresseSuisse adresse = addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2008, 8, 23), null, MockRue.Lausanne.AvenueDesBergieres);
			adresse.setNumeroMaison("186b");

			final Ids ids1 = new Ids();
			ids1.idAdresse = adresse.getId();
			ids1.idTiers = pp.getNumero();
			return ids1;
		});

		request.setMethod("POST");
		request.setRequestURI("/adresses/adresse-close.do");
		request.addParameter(ID_ADRESSE_PARAMETER_NAME, String.valueOf(ids.idAdresse));
		request.addParameter("idTiers", String.valueOf(ids.idTiers));
		request.addParameter("usage", TypeAdresseTiers.COURRIER.name());
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(date(2008, 8, 23)));
		request.addParameter("dateFin", RegDateHelper.dateToDisplayString(date(2015, 12, 31)));
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);

		// vérification de la clôture de l'adresse en base
		doInNewTransactionAndSession(status -> {
			final AdresseTiers adresse = hibernateTemplate.get(AdresseSupplementaire.class, ids.idAdresse);
			assertNotNull(adresse);
			assertFalse(adresse.isAnnule());
			assertEquals(date(2015, 12, 31), adresse.getDateFin());
			assertEquals((Long) ids.idTiers, adresse.getTiers().getNumero());
			return null;
		});
	}

	@Test
	public void testAddAdresseEtrangere() throws Exception {

		final int noIndividu = 282316;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		request.addParameter("numCTB", String.valueOf(ppId));
		request.addParameter("localiteNpa", "Paris");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
		request.addParameter("dateDebut", "12.02.2002");
		request.setMethod("POST");
		request.setRequestURI("/adresses/adresse-add.do");
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);

		// vérification de la création de l'adresse
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(ppId);
			final Set<AdresseTiers> adressesTiers = tiers.getAdressesTiers();
			assertEquals(1, adressesTiers.size());

			final AdresseTiers adresse = adressesTiers.iterator().next();
			assertNotNull(adresse);
			assertFalse(adresse.isAnnule());
			assertEquals(date(2002, 2, 12), adresse.getDateDebut());
			assertNull(adresse.getDateFin());
			assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());
			assertEquals(AdresseEtrangere.class, adresse.getClass());

			final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
			assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
			return null;
		});
	}

	@Test
	public void testAddAdresseSuisse() throws Exception {

		final int noIndividu = 282316;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		request.addParameter("numCTB", String.valueOf(ppId));
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
		request.addParameter("dateDebut", "12.02.2002");
		request.setRequestURI("/adresses/adresse-add.do");
		request.setMethod("POST");
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);

		// vérification de la création de l'adresse
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(ppId);
			final Set<AdresseTiers> adressesTiers = tiers.getAdressesTiers();
			assertEquals(1, adressesTiers.size());

			final AdresseTiers adresse = adressesTiers.iterator().next();
			assertNotNull(adresse);
			assertFalse(adresse.isAnnule());
			assertEquals(date(2002, 2, 12), adresse.getDateDebut());
			assertNull(adresse.getDateFin());
			assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());
			assertEquals(AdresseSuisse.class, adresse.getClass());

			final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
			assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
			return null;
		});
	}

	@Test
	public void testAddAdresseSuisseSansDateDebut() throws Exception {

		final int noIndividu = 282316;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		request.addParameter("numCTB", String.valueOf(ppId));
		request.addParameter("localiteSuisse", "Renens VD");
		request.addParameter("numeroOrdrePoste", "165");
		request.addParameter("typeLocalite", "suisse");
		request.addParameter("usage", "COURRIER");
//		request.addParameter("dateDebut", "12.02.2002");
		request.setRequestURI("/adresses/adresse-add.do");
		request.setMethod("POST");
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);

		final BindingResult br = (BindingResult) model.get(BindingResult.class.getName() + ".editCommand");
		assertNotNull(br);
		assertTrue(br.hasFieldErrors("dateDebut"));

		// vérification de la non-création de l'adresse
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(ppId);
			final Set<AdresseTiers> adressesTiers = tiers.getAdressesTiers();
			assertEquals(0, adressesTiers.size());
			return null;
		});
	}

	@Test
	public void testAddAdresseEtrangereSansDateDebut() throws Exception {

		final int noIndividu = 282316;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		request.addParameter("numCTB", String.valueOf(ppId));
		request.addParameter("localiteNpa", "Paris");
		request.addParameter("typeLocalite", "pays");
		request.addParameter("usage", "COURRIER");
		request.addParameter("paysNpa", "France");
		request.addParameter("paysOFS", "8212");
//		request.addParameter("dateDebut", "12.02.2002");
		request.setMethod("POST");
		request.setRequestURI("/adresses/adresse-add.do");
		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		assertNotNull(model);

		final BindingResult br = (BindingResult) model.get(BindingResult.class.getName() + ".editCommand");
		assertNotNull(br);
		assertTrue(br.hasFieldErrors("dateDebut"));

		// vérification de la non-création de l'adresse
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(ppId);
			final Set<AdresseTiers> adressesTiers = tiers.getAdressesTiers();
			assertEquals(0, adressesTiers.size());
			return null;
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
		doInNewTransactionAndSession(status -> {
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

		request.setRequestURI("/adresses/adresse-add.do");
		request.setMethod("POST");
		handle(request, response);

		// On vérifie que l'adresse saisie a été ajoutée à la fois sur le ménage et sur le principal décédé
		doInNewTransactionAndSession(status -> {
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
		});
	}
}

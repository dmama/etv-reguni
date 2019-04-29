package ch.vd.unireg.tiers;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.tiers.view.NonHabitantCivilView;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CivilEditControllerTest extends WebTestSpring3 {

	private static final String NH_URI = "/civil/nonhabitant/edit.do";

	/**
	 * [UNIREG-2233] Vérifie qu'il n'est pas possible de mettre un nom vide sur un non-habitant
	 */
	@Test
	public void testSubmitPrenomNomVides() throws Exception {

		final Long id = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique nh = addNonHabitant("", "temp", date(1967, 11, 1), Sexe.FEMININ);
				return nh.getNumero();
			}
		});

		// affiche la page de modification du tiers
		{
			request.setMethod("GET");
			request.addParameter("id", id.toString());
			request.setRequestURI(NH_URI);

			final ModelAndView mav = handle(request, response);
			assertNotNull(mav);

			final NonHabitantCivilView view = (NonHabitantCivilView) mav.getModel().get("data");
			assertNotNull(view);
		}

		// essaie de modifier le nom par des espaces -> opération non-autorisée
		{
			request = new MockHttpServletRequest();
			request.setSession(session);
			request.addParameter("id", id.toString());
			request.addParameter("nom", "      ");
			request.setMethod("POST");
			request.setRequestURI(NH_URI);

			final ModelAndView mav = handle(request, response);
			assertNotNull(mav);

			// vérification que l'erreur a bien été catchée et qu'on va afficher un gentil message à l'utilisateur.
			final BeanPropertyBindingResult exception = getBindingResult(mav, "data");
			assertNotNull(exception);
			assertEquals(1, exception.getErrorCount());

			final List<ObjectError> errors = exception.getAllErrors();
			final FieldError error = (FieldError) errors.get(0);
			assertNotNull(error);
			assertEquals("nom", error.getField());
			assertEquals("error.tiers.nom.vide", error.getCode());
		}
	}

	@Test
	public void testOnSubmitWithNom() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("nom", "Kamel");
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		Assert.assertNotNull(model);

		doInNewTransactionAndSession(status -> {
			final List<Tiers> all = tiersDAO.getAll();
			Assert.assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

			final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
			Assert.assertEquals(1, allPP.size());
			final PersonnePhysique nh = allPP.get(0);
			Assert.assertEquals("Kamel", nh.getNom());
			return null;
		});
	}

	@Test
	public void testOnSubmitWithDateNaissancePartielle() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		final String nom = "TestKamel";
		final RegDate dateN = RegDate.get(1956, 11);

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("nom", nom);
		request.addParameter("dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		Assert.assertNotNull(model);

		doInNewTransactionAndSession(status -> {
			final List<Tiers> all = tiersDAO.getAll();
			Assert.assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

			final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
			Assert.assertEquals(1, allPP.size());
			final PersonnePhysique nh = allPP.get(0);
			Assert.assertEquals(nom, nh.getNom());
			Assert.assertEquals(dateN, nh.getDateNaissance());
			return null;
		});
	}

	@Test
	public void testOnSubmitWithWrongDateNaissance() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("dateNaissance", "12/*/.2008");
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		final Map<String, Object> model = mav.getModel();
		Assert.assertNotNull(model);

		doInNewTransactionAndSession(status -> {
			final List<Tiers> all = tiersDAO.getAll();
			Assert.assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

			final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
			Assert.assertEquals(1, allPP.size());
			final PersonnePhysique nh = allPP.get(0);
			Assert.assertNull(nh.getDateNaissance());
			return null;
		});
	}

	@Test
	public void testOnSubmitWithDateNaissance() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("nom", "TestKamel");
		final RegDate dateN = RegDate.get(2008, 4, 12);
		request.addParameter("dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		handle(request, response);
		doInNewTransactionAndSession(status -> {
			final List<Tiers> all = tiersDAO.getAll();
			Assert.assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

			final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
			Assert.assertEquals(1, allPP.size());
			final PersonnePhysique nh = allPP.get(0);
			Assert.assertEquals(dateN, nh.getDateNaissance());
			return null;
		});
	}

	@Test
	public void testModifyNonHabitant() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant(null, "Kamel", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(ppId);
			final PersonnePhysique nh = (PersonnePhysique) tiers;
			Assert.assertEquals("Kamel", nh.getNom());
			Assert.assertEquals(null, nh.getPrenomUsuel());
			return null;
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("nom", "Kamel");
		request.addParameter("prenomUsuel", "toto");
		request.addParameter("tousPrenoms", "toto titi tata");
		request.setMethod("POST");
		request.setRequestURI(NH_URI);
		handle(request, response);

		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(ppId);
			final PersonnePhysique nh = (PersonnePhysique) tiers;
			Assert.assertEquals("Kamel", nh.getNom());
			Assert.assertEquals("toto", nh.getPrenomUsuel());
			Assert.assertEquals("toto titi tata", nh.getTousPrenoms());
			return null;
		});
	}
}

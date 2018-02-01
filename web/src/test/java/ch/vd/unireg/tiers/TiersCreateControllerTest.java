package ch.vd.unireg.tiers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.tiers.view.CreateNonHabitantView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TiersCreateControllerTest extends WebTestSpring3 {

	private static final String NH_URI = "/tiers/nonhabitant/create.do";
	private static final Pattern REDIRECT_VIEW_NAME = Pattern.compile("redirect:/tiers/visu\\.do\\?id=([0-9]+)");

	@Test
	public void testAffichagePageEntree() throws Exception {
		request.setMethod("GET");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final CreateNonHabitantView view = (CreateNonHabitantView) mav.getModel().get("data");
		assertNotNull(view);
		assertNotNull(view.getCivil());
		assertNotNull(view.getComplementCommunication());
		assertNotNull(view.getComplementCoordFinanciere());
	}

	@Test
	public void testOnSubmitWithNom() throws Exception {

		// affichage de la page de base

		request.addParameter("civil.nom", "Kamel");
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final String viewName = mav.getViewName();
		final Matcher matcher = REDIRECT_VIEW_NAME.matcher(viewName);
		assertTrue(viewName, matcher.matches());
		final long id = Long.parseLong(matcher.group(1));

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tiers> all = tiersDAO.getAll();
				assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

				final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
				assertEquals(1, allPP.size());
				final PersonnePhysique nh = allPP.get(0);
				assertEquals("Kamel", nh.getNom());
				assertEquals((Long) id, nh.getNumero());
			}
		});
	}

	@Test
	public void testOnSubmitWithDateNaissancePartielle() throws Exception {

		final String nom = "TestKamel";
		final RegDate dateN = RegDate.get(1956, 11);

		request.addParameter("civil.nom", nom);
		request.addParameter("civil.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final String viewName = mav.getViewName();
		final Matcher matcher = REDIRECT_VIEW_NAME.matcher(viewName);
		assertTrue(viewName, matcher.matches());
		final long id = Long.parseLong(matcher.group(1));

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tiers> all = tiersDAO.getAll();
				assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

				final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
				assertEquals(1, allPP.size());
				final PersonnePhysique nh = allPP.get(0);
				assertEquals(nom, nh.getNom());
				assertEquals(dateN, nh.getDateNaissance());
				assertEquals((Long) id, nh.getNumero());
			}
		});
	}

	@Test
	public void testOnSubmitWithWrongDateNaissance() throws Exception {

		request.addParameter("civil.dateNaissance", "12/*/.2008");
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final BindingResult exception = getBindingResult(mav, "data");
		assertNotNull(exception);
		assertEquals(1, exception.getErrorCount());

		final List<ObjectError> errors = exception.getAllErrors();
		assertEquals(1, errors.size());

		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("civil.dateNaissance", error.getField());
		assertEquals("typeMismatch", error.getCode());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tiers> all = tiersDAO.getAll();
				assertEquals(MockCollectiviteAdministrative.getAll().size(), all.size());

				final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
				assertEquals(0, allPP.size());
			}
		});
	}

	@Test
	public void testOnSubmitWithDateNaissance() throws Exception {

		final RegDate dateN = RegDate.get(2008, 4, 12);
		request.addParameter("civil.nom", "TestKamel");
		request.addParameter("civil.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final String viewName = mav.getViewName();
		final Matcher matcher = REDIRECT_VIEW_NAME.matcher(viewName);
		assertTrue(viewName, matcher.matches());
		final long id = Long.parseLong(matcher.group(1));

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tiers> all = tiersDAO.getAll();
				assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

				final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
				assertEquals(1, allPP.size());
				final PersonnePhysique nh = allPP.get(0);
				assertEquals(dateN, nh.getDateNaissance());
				assertEquals((Long) id, nh.getNumero());
			}
		});
	}

	@Test
	public void testOnSubmitWithTousPrenoms() throws Exception {

		request.addParameter("civil.nom", "Petiot");
		request.addParameter("civil.tousPrenoms", "Pierre Alain Gérard");
		request.addParameter("civil.prenomUsuel", "Alain");
		request.setMethod("POST");
		request.setRequestURI(NH_URI);

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final String viewName = mav.getViewName();
		final Matcher matcher = REDIRECT_VIEW_NAME.matcher(viewName);
		assertTrue(viewName, matcher.matches());
		final long id = Long.parseLong(matcher.group(1));

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tiers> all = tiersDAO.getAll();
				assertEquals(1 + MockCollectiviteAdministrative.getAll().size(), all.size());

				final List<PersonnePhysique> allPP = allTiersOfType(PersonnePhysique.class);
				assertEquals(1, allPP.size());
				final PersonnePhysique nh = allPP.get(0);
				assertEquals((Long) id, nh.getNumero());
				assertEquals("Petiot", nh.getNom());
				assertEquals("Alain", nh.getPrenomUsuel());
				assertEquals("Pierre Alain Gérard", nh.getTousPrenoms());
			}
		});
	}
}

package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CivilEditControllerTest extends WebTest {

	private CivilEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(CivilEditController.class, "civilEditController");
	}

	/**
	 * [UNIREG-2233] Vérifie qu'il n'est pas possible de mettre un nom vide sur un non-habitant
	 */
	@Test
	@NotTransactional
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

			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);

			final TiersEditView view = (TiersEditView) mav.getModel().get("command");
			assertNotNull(view);
		}

		// essaie de modifier le nom par des espaces -> opération non-autorisée
		{
			request = new MockHttpServletRequest();
			request.setSession(session); // note: le form backing object est conservé dans la session
			request.addParameter("tiers.nom", "      ");
			request.setMethod("POST");

			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);

			// vérification que l'erreur a bien été catchée et qu'on va afficher un gentil message à l'utilisateur.
			final BeanPropertyBindingResult exception = getBindingResult(mav);
			assertNotNull(exception);
			assertEquals(1, exception.getErrorCount());

			final List<?> errors = exception.getAllErrors();
			final FieldError error = (FieldError) errors.get(0);
			assertNotNull(error);
			assertEquals("tiers.nom", error.getField());
			assertEquals("error.tiers.nom.vide", error.getCode());
		}
	}

	@Test
	@NotTransactional
	public void testOnSubmitWithNom() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("tiers.nom", "Kamel");
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);
		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tiers> l = tiersDAO.getAll();
				Assert.assertEquals(1, l.size());

				final PersonnePhysique nh = (PersonnePhysique) l.get(0);
				Assert.assertEquals("Kamel", nh.getNom());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testOnSubmitWithDateNaissancePartielle() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final String nom = "TestKamel";
		final RegDate dateN = RegDate.get(1956, 11);

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("tiers.nom", nom);
		request.addParameter("tiers.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);
		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tiers> l = tiersDAO.getAll();
				Assert.assertEquals(1, l.size());
				final PersonnePhysique nh = (PersonnePhysique) l.get(0);
				Assert.assertEquals(nom, nh.getNom());
				Assert.assertEquals(dateN, nh.getDateNaissance());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testOnSubmitWithWrongDateNaissance() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("tiers.dateNaissance", "12/*/.2008");
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);
		request.setMethod("POST");
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		Assert.assertNotNull(model);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tiers> l = tiersDAO.getAll();
				Assert.assertEquals(1, l.size());
				final PersonnePhysique nh = (PersonnePhysique) l.get(0);
				Assert.assertNull(nh.getDateNaissance());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testOnSubmitWithDateNaissance() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alexander", "Kaminski", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("tiers.nom", "TestKamel");
		final RegDate dateN = RegDate.get(2008, 4, 12);
		request.addParameter("tiers.dateNaissance", RegDateHelper.dateToDisplayString(dateN));
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);

		request.setMethod("POST");
		controller.handleRequest(request, response);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Tiers> l = tiersDAO.getAll();
				Assert.assertEquals(1, l.size());
				final PersonnePhysique nh = (PersonnePhysique)l.get(0);
				Assert.assertEquals(dateN, nh.getDateNaissance());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testModifyNonHabitant() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant(null, "Kamel", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Tiers tiers = tiersDAO.get(ppId);
				final PersonnePhysique nh = (PersonnePhysique)tiers;
				Assert.assertEquals("Kamel", nh.getNom());
				Assert.assertEquals(null, nh.getPrenom());
				return null;
			}
		});

		request.addParameter("id", Long.toString(ppId));
		request.addParameter("tiers.nom", "Kamel");
		request.addParameter("tiers.prenom", "toto");
		request.addParameter(TiersEditController.BUTTON_SAVE, TiersEditController.BUTTON_SAVE);
		request.setMethod("POST");
		controller.handleRequest(request, response);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Tiers tiers = tiersDAO.get(ppId);
				final PersonnePhysique nh = (PersonnePhysique)tiers;
				Assert.assertEquals("Kamel", nh.getNom());
				Assert.assertEquals("toto", nh.getPrenom());
				return null;
			}
		});
	}
}

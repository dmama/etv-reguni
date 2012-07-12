package ch.vd.uniregctb.tiers;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.OpenSessionInTestExecutionListener;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Note: pour simuler le context d'exécution exact d'un controlleur web, il faut définir :
 * <ul>
 * <li>un test execution listener <i>OpenSessionInTestExecutionListener</i> sur le test unitaire pour ouvrir la session hibernate comme le
 * fait le <i>OpenSessionInViewInterceptor</i> de unireg-web-common.xml</li>
 * <li>l'annotation <i>NotTransactional</i> au niveau de chaque méthode pour que les annotations <i>Transactional</i> autour des managers de
 * web ferment bien les transactions au retour d'exécution des méthodes</li>
 * </ul>
 *
 * <b>JTA Note:</b> lorsque le test utilise un transaction manager XA, il est nécessaire de faire un flush et un clear de la session
 * Hibernate après un <i>doInNewTransaction</i>. Il semble en effet que JTA associe chaque transaction à la session Hibernate courante.</li>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@TestExecutionListeners(OpenSessionInTestExecutionListener.class)
public class FiscalEditControllerTest extends WebTest {

	private FiscalEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(FiscalEditController.class, "fiscalEditController");
	}

	/**
	 * Cas JIRA UNIREG-573: l'annulation du for principal alors qu'un for secondaire subsiste doit :
	 * <ul>
	 * <li>ne pas être permise</li>
	 * <li>provoquer le réaffichage du formulaire avec des messages d'erreur</li>
	 * </ul>
	 */
	@Test
	public void testAnnuleForPrincipalAvecForSecondaireOuvert() throws Exception {

		class Ids {
			Long ericId;
			Long forPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec un for principal et un for secondaire
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getId();

				ForFiscalPrincipal forPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.forPrincipalId = forPrincipal.getId();
				forPrincipal.setTiers(eric);

				ForFiscalSecondaire forSecondaire = addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER,
						MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				forSecondaire.setTiers(eric);

				// la session hibernate reste ouverte à cause du OpenSessionInTestExecutionListener, on la flush()
				// et on la clear() à la main ici pour qu'elle soit bien vide avant l'appel à handleRequest()
				hibernateTemplate.flush();
				hibernateTemplate.clear();
				return null;
			}
		});

		// simulation de l'annulation du for principal
		request.setMethod("POST");
		request.addParameter(AbstractTiersController.TIERS_ID_PARAMETER_NAME, ids.ericId.toString());
		request.addParameter(AbstractSimpleFormController.PARAMETER_TARGET, FiscalEditController.TARGET_ANNULER_FOR);
		request.addParameter(AbstractSimpleFormController.PARAMETER_EVENT_ARGUMENT, ids.forPrincipalId.toString());
		final ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);

		// vérification que l'erreur a bien été catchée et qu'on va afficher un gentil message à l'utilisateur.
		final BeanPropertyBindingResult exception = getBindingResult(mav);
		assertNotNull(exception);
		assertEquals(1, exception.getErrorCount());

		final List<?> errors = exception.getAllErrors();
		final ObjectError error = (ObjectError) errors.get(0);
		assertNotNull(error);
		assertEquals("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le 01.01.2000", error
				.getDefaultMessage());
	}

	/**
	 * Case JIRA UNIREG-586: l'annulation d'un for fiscal principal doit réouvrir le for précédent s'il celui-ci est adjacent.
	 */
	@Test
	public void testAnnuleForPrincipalAvecPrecedentAdjacent() throws Exception {

		class Ids {
			Long ericId;
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux adjacent
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getId();

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 3, 31),
						MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 4, 1), MotifFor.DEMENAGEMENT_VD,
						MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);

				// la session hibernate reste ouverte à cause du OpenSessionInTestExecutionListener, on la flush()
				// et on la clear() à la main ici pour qu'elle soit bien vide avant l'appel à handleRequest()
				hibernateTemplate.flush();
				hibernateTemplate.clear();
				return null;
			}
		});

		// simulation de l'annulation du second for principal
		request.setMethod("POST");
		request.addParameter(AbstractTiersController.TIERS_ID_PARAMETER_NAME, ids.ericId.toString());
		request.addParameter(AbstractSimpleFormController.PARAMETER_TARGET, FiscalEditController.TARGET_ANNULER_FOR);
		request.addParameter(AbstractSimpleFormController.PARAMETER_EVENT_ARGUMENT, ids.secondForPrincipalId.toString());
		final ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);

		// vérification qu'il n'y a pas eu d'erreur de traitement
		final BeanPropertyBindingResult exception = getBindingResult(mav);
		assertNotNull(exception);
		assertEquals(0, exception.getErrorCount());

		// vérification que le second for est bien annulé
		final ForFiscalPrincipal secondForPrincipal = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class,
				ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);
		assertTrue(secondForPrincipal.isAnnule());

		// vérification que le premier for est bien ré-ouvert
		final ForFiscalPrincipal premierForPrincipal = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class,
				ids.premierForPrincipalId);
		assertNotNull(premierForPrincipal);
		assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
		assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
		assertNull(premierForPrincipal.getDateFin());
		assertNull(premierForPrincipal.getMotifFermeture());
		assertFalse(premierForPrincipal.isAnnule());
	}

	/**
	 * Case JIRA UNIREG-586: l'annulation d'un for fiscal principal ne doit pas réouvrir le for précédent s'il celui-ci n'est pas adjacent.
	 */
	@Test
	public void testAnnuleForPrincipalAvecPrecedentNonAdjacents() throws Exception {

		class Ids {
			Long ericId;
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux non-adjacents
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getId();

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 2, 28),
						MotifFor.DEPART_HC, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 11, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);

				// la session hibernate reste ouverte à cause du OpenSessionInTestExecutionListener, on la flush()
				// et on la clear() à la main ici pour qu'elle soit bien vide avant l'appel à handleRequest()
				hibernateTemplate.flush();
				hibernateTemplate.clear();
				return null;
			}
		});

		// simulation de l'annulation du second for principal
		request.setMethod("POST");
		request.addParameter(AbstractTiersController.TIERS_ID_PARAMETER_NAME, ids.ericId.toString());
		request.addParameter(AbstractSimpleFormController.PARAMETER_TARGET, FiscalEditController.TARGET_ANNULER_FOR);
		request.addParameter(AbstractSimpleFormController.PARAMETER_EVENT_ARGUMENT, ids.secondForPrincipalId.toString());
		final ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);

		// vérification qu'il n'y a pas eu d'erreur de traitement
		final BeanPropertyBindingResult exception = getBindingResult(mav);
		assertNotNull(exception);
		assertEquals(0, exception.getErrorCount());

		// vérification que le second for est bien annulé
		final ForFiscalPrincipal secondForPrincipal = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class,
				ids.secondForPrincipalId);
		assertNotNull(secondForPrincipal);
		assertTrue(secondForPrincipal.isAnnule());

		// vérification que le premier for n'est pas ré-ouvert
		final ForFiscalPrincipal premierForPrincipal = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class,
				ids.premierForPrincipalId);
		assertNotNull(premierForPrincipal);
		assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
		assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
		assertEquals(date(2008, 2, 28), premierForPrincipal.getDateFin());
		assertEquals(MotifFor.DEPART_HC, premierForPrincipal.getMotifFermeture());
		assertFalse(premierForPrincipal.isAnnule());
	}
}

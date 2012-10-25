package ch.vd.uniregctb.couple;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class CoupleControllerTest extends WebTestSpring3 {

	private TiersService tiersService;

	private final static String DB_UNIT_FILE = "classpath:ch/vd/uniregctb/couple/CoupleControllerTest.xml";

	protected Long numeroPP1 = 12300002L;
	protected Long numeroPP2 = 12300003L;


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		loadDatabase(DB_UNIT_FILE);
	}

	@Test
	public void showForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("pp1", numeroPP1.toString());
		request.setRequestURI("/couple/create.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final CoupleView coupleView = (CoupleView) mav.getModel().get("command");
		assertNotNull(coupleView);
		assertEquals(numeroPP1, coupleView.getPp1Id());
	}

	@Test
	public void onSubmitSansDate() throws Exception {

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique nhab1 = (PersonnePhysique)tiersDAO.get(numeroPP1);
				assertNotNull(nhab1);

				// on s'assure que la personne n'est pas déjà mariée
				final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, RegDate.get());
				assertNull(etc);
				return null;
			}
		});

		// Une requête sans la date de début
		request.setMethod("POST");
		request.addParameter("pp1Id", numeroPP1.toString());
		request.addParameter("pp2Id", numeroPP2.toString());
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur la date de début a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("dateDebut", error.getField());
		assertEquals("error.date.debut.vide", error.getCode());

		final CoupleView coupleView = (CoupleView) mav.getModel().get("command");
		assertNotNull(coupleView);

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique nhab1 = (PersonnePhysique)tiersDAO.get(numeroPP1);
				assertNotNull(nhab1);

				// On vérifie que la personne n'est toujours pas mariée
				EnsembleTiersCouple etc2 = tiersService.getEnsembleTiersCouple(nhab1, RegDate.get());
				assertNull(etc2);
				return null;
			}
		});
	}

	/**
	 * Une date de mariage définie avant la date d'ouverture du for principal -> erreur
	 * <p>
	 * <b>UPDATE msi 19.05.2009:</b> le cas d'un événement de mariage qui arrive plus tard que l'événement d'arrivée est maintenant traité
	 * de manière automatique (le for du principal est annulé pour être ouvert à la date du mariage sur le couple). Par extension, il est
	 * possible de marier deux personnes avant la date d'ouverture du for principal.
	 */
	@Test
	public void onSubmitAvecDateAvantForFiscal() throws Exception {

		final Date dateMariage = DateHelper.getDate(2007, 2, 12);

		// Simule l'envoi de la requête au serveur
		request.setMethod("POST");
		request.addParameter("pp1Id", numeroPP1.toString());
		request.addParameter("pp2Id", numeroPP2.toString());
		request.addParameter("dateDebut", DateHelper.dateToDisplayString(dateMariage));
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// Vérifie que le couple a été créé dans la base
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique nhab1 = (PersonnePhysique)tiersDAO.get(numeroPP1);
				assertNotNull(nhab1);

				EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, null);
				assertNotNull(etc);
				assertNotNull(etc.getMenage());
				return null;
			}
		});
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void onSubmitAvecDateApresForFiscal() throws Exception {

		final RegDate dateMariage = RegDate.get(2008, 3, 12);

		request.setMethod("POST");
		request.addParameter("pp1Id", numeroPP1.toString());
		request.addParameter("pp2Id", numeroPP2.toString());
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateMariage));
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Vérifie que le couple est créé
				PersonnePhysique nhab1 = (PersonnePhysique)tiersDAO.get(numeroPP1);
				assertNotNull(nhab1);

				// Prends le couple très loin dans le passé, pour être sur de le récupérer
				EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, dateMariage);
				assertNotNull(etc);
				assertNotNull(etc.getMenage());
				return null;
			}
		});
	}

}

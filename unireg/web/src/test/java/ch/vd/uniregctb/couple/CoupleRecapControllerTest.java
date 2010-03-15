package ch.vd.uniregctb.couple;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.Map;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import junit.framework.Assert;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class CoupleRecapControllerTest extends AbstractCoupleControllerTest {

	private CoupleRecapController controller;
	private TiersService tiersService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(CoupleRecapController.class, "coupleRecapController");
		tiersService = getBean(TiersService.class, "tiersService");
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void showForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numeroPP1", numeroPP1.toString());
		request.addParameter("numeroPP2", numeroPP2.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void onSubmitSansDate() throws Exception {

		PersonnePhysique nhab1 = (PersonnePhysique)tiersDAO.get(numeroPP1);
		assertNotNull(nhab1);

		// Prends le couple très loin dans le passé, pour être sur de le récupérer
		EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, RegDate.get());
		assertNull(etc);

		//2008-02-12
		request.setMethod("POST");
		request.addParameter("numeroPP1", numeroPP1.toString());
		request.addParameter("numeroPP2", numeroPP2.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// Prends le couple très loin dans le passé, pour être sur de le récupérer
		EnsembleTiersCouple etc2 = tiersService.getEnsembleTiersCouple(nhab1, RegDate.get());
		assertNull(etc2);
	}

	/**
	 * Une date de mariage définie avant la date d'ouverture du for principal -> erreur
	 * <p>
	 * <b>UPDATE msi 19.05.2009:</b> le cas d'un événement de mariage qui arrive plus tard que l'événement d'arrivée est maintenant traité
	 * de manière automatique (le for du principal est annulé pour être ouvert à la date du mariage sur le couple). Par extension, il est
	 * possible de marier deux personnes avant la date d'ouverture du for principal.
	 */
	@Test
	@NotTransactional
	public void onSubmitAvecDateAvantForFiscal() throws Exception {

		final Date dateMariage = DateHelper.getDate(2007, 02, 12);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Simule l'envoi de la requête au serveur
				request.setMethod("POST");
				request.addParameter("numeroPP1", numeroPP1.toString());
				request.addParameter("numeroPP2", numeroPP2.toString());
				request.addParameter("dateDebut", DateHelper.dateToDisplayString(dateMariage));
				ModelAndView mav = controller.handleRequest(request, response);
				Map<?, ?> model = mav.getModel();
				assertNotNull(model);
				return null;
			}
		});

		// Vérifie que le couple a été créé dans la base
		doInNewTransaction(new TxCallback() {
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
	@NotTransactional
	public void onSubmitAvecDateApresForFiscal() throws Exception {

		final RegDate dateMariage = RegDate.get(2008, 03, 12);

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				request.setMethod("POST");
				request.addParameter("numeroPP1", numeroPP1.toString());
				request.addParameter("numeroPP2", numeroPP2.toString());
				request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateMariage));
				ModelAndView mav = controller.handleRequest(request, response);
				Map<?, ?> model = mav.getModel();
				assertNotNull(model);
				return null;
			}
		});

		// On fait le test dans 2 transactions pour que les rapports soient re-chargés par Hibernate

		doInNewTransaction(new TxCallback() {
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

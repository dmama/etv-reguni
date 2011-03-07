package ch.vd.uniregctb.evenement;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandler;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.MockEvenementFiscalSender;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.metier.MetierService;

public abstract class AbstractEvenementHandlerTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(AbstractEvenementHandlerTest.class);

	/**
	 * Une instance du gestionnaire d'événement.
	 */
	protected EvenementCivilHandler evenementCivilHandler;

	protected GlobalTiersIndexer indexer;
	protected MockEvenementFiscalSender eventSender;
	protected MetierService metierService;

	protected EvenementCivilContext context;
	protected DataEventService dataEventService;
	protected EvenementFiscalService evenementFiscalService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evenementCivilHandler = getBean(EvenementCivilHandler.class, getHandlerBeanName());
		indexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		eventSender = getBean(MockEvenementFiscalSender.class, "evenementFiscalSender");
		metierService = getBean(MetierService.class, "metierService");
		dataEventService = getBean(DataEventService.class, "dataEventService");
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		eventSender.count = 0;

		context = new EvenementCivilContext(serviceCivil, serviceInfra, dataEventService, tiersService, indexer, metierService, tiersDAO, null, evenementFiscalService, false);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
		if (serviceCivil != null) {
			serviceCivil.tearDown();
		}
	}

	public String getHandlerBeanName() {

		String cn = getClass().getSimpleName();
		String first = cn.substring(0, 1); // Premiere lettre
		first = first.toLowerCase(); // En minuscule
		cn = cn.substring(1, cn.length()-4); // Suppression de "Test"
		cn = first + cn;
		return cn;
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return getBean(EvenementFiscalService.class,"evenementFiscalService");
	}

	protected void launchEvent(final EvenementCivilInterne evtCivil, final List<EvenementCivilExterneErreur> erreurs, final List<EvenementCivilExterneErreur> warnings) throws Exception {
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				evtCivil.checkCompleteness(erreurs, warnings);
				if (erreurs.isEmpty()) {
					evtCivil.validate(erreurs, warnings);
					if (erreurs.isEmpty()) {
						evtCivil.handle(warnings);
					}
				}
				return null;
			}
		});
	}

	protected void assertSansErreurNiWarning(EvenementCivilInterne evt) throws Exception {
		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		launchEvent(evt, erreurs, warnings);
		Assert.assertEquals(0, erreurs.size());
		Assert.assertEquals(0, warnings.size());
	}

	protected void assertErreurs(EvenementCivilInterne evt, List<String> messagesErreurs) throws Exception {
		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		try {
			launchEvent(evt, erreurs, warnings);
		}
		catch (EvenementCivilHandlerException e) {
			erreurs.add(new EvenementCivilExterneErreur(e));
		}

		Assert.assertEquals(messagesErreurs.size(), erreurs.size());
		Assert.assertEquals(0, warnings.size());

		for (int i = 0 ; i < messagesErreurs.size() ; ++ i) {
			final String expected = messagesErreurs.get(i);
			final EvenementCivilExterneErreur erreurTrouvee = erreurs.get(i);
			Assert.assertEquals("Index " + i, expected, erreurTrouvee.getMessage());
		}
	}
}

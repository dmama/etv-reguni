package ch.vd.uniregctb.evenement.civil.interne;

import java.util.List;

import junit.framework.Assert;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.MockEvenementFiscalSender;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.metier.MetierService;

public abstract class AbstractEvenementCivilInterneTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(AbstractEvenementCivilInterneTest.class);

	protected GlobalTiersIndexer indexer;
	protected MockEvenementFiscalSender eventSender;
	protected MetierService metierService;

	protected EvenementCivilContext context;
	protected DataEventService dataEventService;
	protected EvenementFiscalService evenementFiscalService;
	protected EvenementCivilOptions options;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		indexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		eventSender = getBean(MockEvenementFiscalSender.class, "evenementFiscalSender");
		metierService = getBean(MetierService.class, "metierService");
		dataEventService = getBean(DataEventService.class, "dataEventService");
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		eventSender.count = 0;

		context = new EvenementCivilContext(serviceCivil, serviceInfra, dataEventService, tiersService, indexer, metierService, tiersDAO, null, evenementFiscalService);
		options = buildOptions();
	}

	protected EvenementCivilOptions buildOptions() {
		return new EvenementCivilOptions(false);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
		if (serviceCivil != null) {
			serviceCivil.tearDown();
		}
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	protected void launchEvent(final EvenementCivilInterne evtCivil, final EvenementCivilErreurCollector erreurs, final EvenementCivilWarningCollector warnings) throws Exception {
		evtCivil.validate(erreurs, warnings);
		if (!erreurs.hasErreurs()) {
			evtCivil.handle(warnings);
		}
	}

	protected static MessageCollector buildMessageCollector() {
		return new MessageCollector();
	}

	protected void assertSansErreurNiWarning(EvenementCivilInterne evt) throws Exception {
		final EvenementCivilErreurCollector erreurs = buildMessageCollector();
		final EvenementCivilWarningCollector warnings = buildMessageCollector();
		launchEvent(evt, erreurs, warnings);
		Assert.assertFalse(erreurs.hasErreurs());
		Assert.assertFalse(warnings.hasWarnings());
	}

	protected void assertErreurs(EvenementCivilInterne evt, List<String> messagesErreurs) throws Exception {
		final MessageCollector erreurs = buildMessageCollector();
		final MessageCollector warnings = buildMessageCollector();
		try {
			launchEvent(evt, erreurs, warnings);
		}
		catch (EvenementCivilException e) {
			erreurs.addErreur(e);
		}

		Assert.assertEquals(messagesErreurs.size(), erreurs.getErreurs().size());
		Assert.assertFalse(warnings.hasWarnings());

		for (int i = 0 ; i < messagesErreurs.size() ; ++ i) {
			final String expected = messagesErreurs.get(i);
			final EvenementCivilErreur erreurTrouvee = erreurs.getErreurs().get(i);
			Assert.assertEquals("Index " + i, expected, erreurTrouvee.getMessage());
		}
	}
}

package ch.vd.uniregctb.evenement.civil.interne;

import java.util.List;

import org.junit.Assert;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.common.EvenementErreur;
import ch.vd.uniregctb.evenement.fiscal.CollectingEvenementFiscalSender;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.parametrage.ParametreAppService;

public abstract class AbstractEvenementCivilInterneTest extends BusinessTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvenementCivilInterneTest.class);

	protected CollectingEvenementFiscalSender eventSender;
	protected MetierService metierService;

	protected EvenementCivilContext context;
	protected DataEventService dataEventService;
	protected EvenementFiscalService evenementFiscalService;
	protected EvenementCivilOptions options;
	protected ParametreAppService parametreAppService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		eventSender = getBean(CollectingEvenementFiscalSender.class, "evenementFiscalSender");
		metierService = getBean(MetierService.class, "metierService");
		dataEventService = getBean(DataEventService.class, "dataEventService");
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		eventSender.reset();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		context = new EvenementCivilContext(serviceCivil, serviceInfra, dataEventService, tiersService, globalTiersIndexer, metierService, tiersDAO, adresseService, evenementFiscalService, parametreAppService);
		options = buildOptions();
	}

	protected EvenementCivilOptions buildOptions() {
		return new EvenementCivilOptions(false);
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
			final EvenementErreur erreurTrouvee = erreurs.getErreurs().get(i);
			Assert.assertEquals("Index " + i, expected, erreurTrouvee.getMessage());
		}
	}
}

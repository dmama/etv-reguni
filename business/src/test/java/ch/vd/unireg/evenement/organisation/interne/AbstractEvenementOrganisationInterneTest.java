package ch.vd.unireg.evenement.organisation.interne;

import java.util.List;

import org.junit.Assert;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.evenement.fiscal.CollectingEvenementFiscalSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationService;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;

public abstract class AbstractEvenementOrganisationInterneTest extends BusinessTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvenementOrganisationInterneTest.class);

	protected CollectingEvenementFiscalSender eventSender;
	protected MetierServicePM metierService;

	protected EvenementOrganisationContext context;
	protected EvenementOrganisationService evenementOrganisationService;
	protected DataEventService dataEventService;
	protected EvenementFiscalService evenementFiscalService;
	protected AssujettissementService assujettissementService;
	protected AppariementService appariementService;
	protected EvenementOrganisationOptions options;
	protected ParametreAppService parametreAppService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evenementOrganisationService = getBean(EvenementOrganisationService.class, "evtOrganisationService");
		eventSender = getBean(CollectingEvenementFiscalSender.class, "evenementFiscalSender");
		metierService = getBean(MetierServicePM.class, "metierServicePM");
		dataEventService = getBean(DataEventService.class, "dataEventService");
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		appariementService = getBean(AppariementService.class, "appariementService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		eventSender.reset();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		context = new EvenementOrganisationContext(serviceOrganisation, evenementOrganisationService, serviceInfra, regimeFiscalService, dataEventService, tiersService, globalTiersIndexer, metierService, tiersDAO, adresseService, evenementFiscalService, assujettissementService, appariementService, parametreAppService);
		options = buildOptions();
	}

	protected EvenementOrganisationOptions buildOptions() {
		return new EvenementOrganisationOptions();
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	protected void launchEvent(final EvenementOrganisationInterne evtOrganisation, final EvenementOrganisationErreurCollector erreurs, final EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws Exception {
		evtOrganisation.validate(erreurs, warnings, suivis);
		if (!erreurs.hasErreurs()) {
			evtOrganisation.handle(warnings, suivis);
		}
	}

	protected static MessageCollector buildMessageCollector() {
		return new MessageCollector();
	}

	protected void assertSansErreurNiWarning(EvenementOrganisationInterne evt) throws Exception {
		final EvenementOrganisationErreurCollector erreurs = buildMessageCollector();
		final EvenementOrganisationWarningCollector warnings = buildMessageCollector();
		final EvenementOrganisationSuiviCollector suivis = buildMessageCollector();
		launchEvent(evt, erreurs, warnings, suivis);
		Assert.assertFalse(erreurs.hasErreurs());
		Assert.assertFalse(warnings.hasWarnings());
	}

	protected void assertErreurs(EvenementOrganisationInterne evt, List<String> messagesErreurs) throws Exception {
		final MessageCollector erreurs = buildMessageCollector();
		final MessageCollector warnings = buildMessageCollector();
		final EvenementOrganisationSuiviCollector suivis = buildMessageCollector();
		try {
			launchEvent(evt, erreurs, warnings, suivis);
		}
		catch (EvenementOrganisationException e) {
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

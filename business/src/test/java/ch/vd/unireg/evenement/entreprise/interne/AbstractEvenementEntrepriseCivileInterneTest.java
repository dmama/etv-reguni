package ch.vd.unireg.evenement.entreprise.interne;

import java.util.List;

import org.junit.Assert;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.fiscal.CollectingEvenementFiscalSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;

public abstract class AbstractEvenementEntrepriseCivileInterneTest extends BusinessTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvenementEntrepriseCivileInterneTest.class);

	protected CollectingEvenementFiscalSender eventSender;
	protected MetierServicePM metierService;

	protected EvenementEntrepriseContext context;
	protected EvenementEntrepriseService evenementEntrepriseService;
	protected CivilDataEventNotifier civilDataEventNotifier;
	protected EvenementFiscalService evenementFiscalService;
	protected AssujettissementService assujettissementService;
	protected AppariementService appariementService;
	protected EvenementEntrepriseOptions options;
	protected ParametreAppService parametreAppService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evenementEntrepriseService = getBean(EvenementEntrepriseService.class, "evtEntrepriseService");
		eventSender = getBean(CollectingEvenementFiscalSender.class, "evenementFiscalSender");
		metierService = getBean(MetierServicePM.class, "metierServicePM");
		civilDataEventNotifier = getBean(CivilDataEventNotifier.class, "civilDataEventNotifier");
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		appariementService = getBean(AppariementService.class, "appariementService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		eventSender.reset();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		context = new EvenementEntrepriseContext(serviceEntreprise, evenementEntrepriseService, serviceInfra, regimeFiscalService, civilDataEventNotifier, tiersService, globalTiersIndexer, metierService, tiersDAO, adresseService, evenementFiscalService, assujettissementService, appariementService, parametreAppService,
		                                         audit);
		options = buildOptions();
	}

	protected EvenementEntrepriseOptions buildOptions() {
		return new EvenementEntrepriseOptions();
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	protected void launchEvent(final EvenementEntrepriseInterne evtEntreprise, final EvenementEntrepriseErreurCollector erreurs, final EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws Exception {
		evtEntreprise.validate(erreurs, warnings, suivis);
		if (!erreurs.hasErreurs()) {
			evtEntreprise.handle(warnings, suivis);
		}
	}

	protected static MessageCollector buildMessageCollector() {
		return new MessageCollector();
	}

	protected void assertSansErreurNiWarning(EvenementEntrepriseInterne evt) throws Exception {
		final EvenementEntrepriseErreurCollector erreurs = buildMessageCollector();
		final EvenementEntrepriseWarningCollector warnings = buildMessageCollector();
		final EvenementEntrepriseSuiviCollector suivis = buildMessageCollector();
		launchEvent(evt, erreurs, warnings, suivis);
		Assert.assertFalse(erreurs.hasErreurs());
		Assert.assertFalse(warnings.hasWarnings());
	}

	protected void assertErreurs(EvenementEntrepriseInterne evt, List<String> messagesErreurs) throws Exception {
		final MessageCollector erreurs = buildMessageCollector();
		final MessageCollector warnings = buildMessageCollector();
		final EvenementEntrepriseSuiviCollector suivis = buildMessageCollector();
		try {
			launchEvent(evt, erreurs, warnings, suivis);
		}
		catch (EvenementEntrepriseException e) {
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

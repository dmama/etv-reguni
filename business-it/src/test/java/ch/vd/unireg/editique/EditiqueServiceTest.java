package ch.vd.uniregctb.editique;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.editique.impl.EditiqueCompositionServiceImpl;
import ch.vd.uniregctb.editique.impl.EditiqueRetourImpressionStorageServiceImpl;
import ch.vd.uniregctb.editique.impl.EditiqueServiceImpl;
import ch.vd.uniregctb.editique.impl.EvenementEditiqueEsbHandler;
import ch.vd.uniregctb.editique.impl.EvenementEditiqueSenderImpl;
import ch.vd.uniregctb.editique.impl.LegacyEditiqueHelperImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.GentilEsbMessageListenerContainer;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tache.ImpressionNouveauxDossiersHelperImpl;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Ce teste vérifie que l'impression online fonctionne bien avec l'éditique service.
 */
@SuppressWarnings({"JavaDoc"})
public class EditiqueServiceTest extends BusinessItTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueServiceTest.class);

	private static final String OUTPUT_QUEUE = "imprimer";

	private EditiqueCompositionServiceImpl composition;
	private EditiqueRetourImpressionStorageServiceImpl storageService;
	private GentilEsbMessageListenerContainer listener;
	private static final int RECEIVE_TIMEOUT = 60;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// On récupère le maximum de beans dans le context
		final EsbJmsTemplate esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		final SituationFamilleService sfService = getBean(SituationFamilleService.class, "situationFamilleService");
		final EsbJmsTemplate noTxEsbTemplate = getBean(EsbJmsTemplate.class, "noTxEsbJmsTemplate");
		final StatsService statsService = getBean(StatsService.class, "statsService");

		// On setup à la main le reste (= le mininum de beans pour faire passer le test)
		final String INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.editique.input");

		final EvenementEditiqueSenderImpl sender = new EvenementEditiqueSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setNoTxEsbTemplate(noTxEsbTemplate);
		sender.setServiceDestinationImpression(OUTPUT_QUEUE);
		sender.setServiceReplyTo(INPUT_QUEUE);

		storageService = new EditiqueRetourImpressionStorageServiceImpl();
		storageService.setStatsService(statsService);
		storageService.setCleanupPeriod(20);
		storageService.afterPropertiesSet();

		final EvenementEditiqueEsbHandler handler = new EvenementEditiqueEsbHandler();
		handler.setStorageService(storageService);

		listener = new GentilEsbMessageListenerContainer();
		listener.setEsbTemplate(esbTemplate);
		listener.setTransactionManager(transactionManager);
		listener.setHandler(handler);
		listener.setDestinationName(INPUT_QUEUE);
		listener.afterPropertiesSet();

		final EditiqueServiceImpl service = new EditiqueServiceImpl();
		service.setSender(sender);
		service.setRetourImpressionStorage(storageService);
		service.setSyncReceiveTimeout(RECEIVE_TIMEOUT);

		final LegacyEditiqueHelperImpl editiqueHelper = new LegacyEditiqueHelperImpl();
		editiqueHelper.setAdresseService(adresseService);
		editiqueHelper.setInfraService(infraService);

		final ImpressionNouveauxDossiersHelperImpl dossiersHelper = new ImpressionNouveauxDossiersHelperImpl();
		dossiersHelper.setLegacyEditiqueHelper(editiqueHelper);
		dossiersHelper.setTiersService(tiersService);
		dossiersHelper.setServiceInfrastructureService(infraService);
		dossiersHelper.setSituationFamilleService(sfService);

		composition = new EditiqueCompositionServiceImpl();
		composition.setImpressionNouveauxDossiersHelper(dossiersHelper);
		composition.setEditiqueService(service);

		AuthenticationHelper.pushPrincipal("EditiqueServiceTest");
	}

	@Override
	public void onTearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
		listener.destroy();
		storageService.destroy();
		super.onTearDown();
	}

	@Ignore(value = "Editique est à 99% du temps indisponible dans l'environnement de test, tant pis !")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testImprimeNouveauxDossiers() throws Exception {

		final PersonnePhysique jose = addNonHabitant("José", "Papenddrum", date(1972, 3, 4), Sexe.MASCULIN);
		addForPrincipal(jose, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Renens);

		final EditiqueResultat resultat = composition.imprimeNouveauxDossiers(Collections.singletonList(jose));
		assertNotNull("Aucun document reçu en retour après " + RECEIVE_TIMEOUT + " secondes", resultat);
		assertInstanceOf(EditiqueResultatDocument.class, resultat);
		assertEquals("application/pdf", ((EditiqueResultatDocument) resultat).getContentType());
		assertTrue(resultat.getIdDocument().startsWith(String.format("%09d", jose.getId())));
	}
}

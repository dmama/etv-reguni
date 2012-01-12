package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.webscreenshot.WebScreenshot;
import ch.vd.registre.webscreenshot.WebScreenshotDoc;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.registre.base.date.DateRangeHelper.Range;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

// Pour générer des screenshots des assujettissements :
//  - activer les annotations ci-dessous
//  - commenter l'appel à 'resetAuthentication' dans AbstractSpringTest.onTearDown()
//  - ajouter 'return false;' à la méthode 'SecurityProviderCache.estControle'.
//  - démarrer l'application web
//  - démarrer sélenium avec la commande : java -jar ./.m2/repository/org/seleniumhq/selenium/server/selenium-server/1.0.3/selenium-server-1.0.3-standalone.jar -browserSessionReuse
//@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
//@WebScreenshotTestListenerConfig(baseUrl = "http://localhost:8080", browserStartCommand = "*firefox /usr/lib/firefox-3.6.13/firefox", outputDir = "/home/msi/bidon/assujettissements")
//@TestExecutionListeners(value = {DependencyInjectionTestExecutionListener.class,
//		DirtiesContextTestExecutionListener.class,
//		WebScreenshotTransactionalTestExecutionListener.class},
//		inheritListeners = false)
@SuppressWarnings({"JavaDoc", "deprecation"})
public class AssujettissementServiceTest extends MetierTest {

	private AssujettissementService service;
	
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(AssujettissementService.class, "assujettissementService");
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000001&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineAucunFor() throws Exception {
		final Contribuable paul = createContribuableSansFor(10000001L);
		assertEmpty(service.determine(paul, 2008));
		assertEmpty(service.determine(paul, RANGE_2002_2010, true));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000002&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineUnForSimple() throws Exception {

		final Contribuable paul = createUnForSimple(10000002L);
		List<Assujettissement> list = service.determine(paul, 2008);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));

		list = service.determine(paul, RANGE_2002_2010, true);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(2002, 1, 1), date(2010, 12, 31), null, null, list.get(0));
	}

	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000003&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000004&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000005&print=true&title=${methodName}&description=Situation%20du%20Couple"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMenageCommunMarieDansLAnnee() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMarie(10000003L, 10000004L, 10000005L, date(2008, 7, 1));

		// 2007
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), 2007);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), 2007);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), 2007));
		}
		
		// 2008
		{
			assertEmpty(service.determine(ensemble.getPrincipal(), 2008));
			assertEmpty(service.determine(ensemble.getConjoint(), 2008));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), 2008);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}

		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}
	}

	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMenageCommunMarieAu1erJanvier() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMarie(10000006L, 10000007L, 10000008L, date(2009, 1, 1));

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), 2008));
		}

		// 2009
		{
			assertEmpty(service.determine(ensemble.getPrincipal(), 2009));
			assertEmpty(service.determine(ensemble.getConjoint(), 2009));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), 2009);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}

		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2009, 1, 1), date(2010, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}
	}

	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000009&print=true&title=${methodName}&description=Situation%20de%20Monsieur", 
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000010&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000011&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMenageCommunDivorceDansLAnnee() throws Exception {

		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDivorce = date(2008, 7, 1);
		final EnsembleTiersCouple ensemble = createMenageCommunDivorce(10000009L, 10000010L, 10000011L, dateMariage, dateDivorce);

		// 2007
		{
			assertEmpty(service.determine(ensemble.getPrincipal(), 2007));
			assertEmpty(service.determine(ensemble.getConjoint(), 2007));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), 2007);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), 2008));
		}
		
		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}
	}
	
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000012&print=true&title=${methodName}&description=Situation%20de%20Monsieur", 
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000013&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000014&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMenageCommunDivorceAu1erJanvier() throws Exception {

		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDivorce = date(2008, 7, 1);
		final EnsembleTiersCouple ensemble = createMenageCommunDivorce(10000012L, 10000013L, 10000014L, dateMariage, dateDivorce);

		// 2007
		{
			assertEmpty(service.determine(ensemble.getPrincipal(), 2007));
			assertEmpty(service.determine(ensemble.getConjoint(), 2007));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), 2007);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), 2008));
		}
		
		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = service.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}
	}

	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000015&print=true&title=${methodName}&description=Situation%20de%20Monsieur", 
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000016&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000017&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMenageCommunMarieEtDivorceDansLAnnee() throws Exception {

		final RegDate dateMariage = date(2008, 3, 1);
		final RegDate dateDivorce = date(2008, 11, 15);
		final EnsembleTiersCouple ensemble = createMenageCommunDivorce(10000015L, 10000016L, 10000017L, dateMariage, dateDivorce);

		// mariage et divorce dans la même année -> aucun effet
		
		// 2007
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), 2007);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), 2007);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, null, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), 2007));
		}

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), 2008));
		}

		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = service.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2002, 1, 1), date(2010, 12, 31), null, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2002, 1, 1), date(2010, 12, 31), null, null, assujetConjoint.get(0));

			assertEmpty(service.determine(ensemble.getMenage(), RANGE_2002_2010, true));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@WebScreenshotDoc(description = "[UNIREG-2432] Vérifie qu'un contribuable VD avec immeuble qui se marie n'est plus assujetti l'année de son mariage (cas fictif)")
	public void testDetermineMariageVaudoisAvecImmeuble() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMariageVDImmeuble(10000006L, 10000007L, 10000008L, date(2009, 11, 1));
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();
		final MenageCommun menage = ensemble.getMenage();

		// 2008 : le principal et le conjoint doivent être assujettis normalement
		{
			final List<Assujettissement> assujetPrincipal = service.determine(principal, 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(conjoint, 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			assertEmpty(service.determine(menage, 2008)); // mariage pas encore actif
		}
		
		// 2009 : le principal et le conjoint ne doivent plus être assujettis, mais le ménage doit l'être
		{
			assertEmpty(service.determine(principal, 2009)); // immeuble transféré sur le ménage
			assertEmpty(service.determine(conjoint, 2009)); // aucun for vaudois

			final List<Assujettissement> assujettissements = service.determine(menage, 2009);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujettissements.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@WebScreenshotDoc(description = "[UNIREG-2432] Vérifie qu'un ménage commun VD avec immeuble qui se divorce n'est plus assujetti l'année du divorce (cas fictif)")
	public void testDetermineDivorceVaudoisAvecImmeuble() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunDivorceVDImmeuble(10000006L, 10000007L, 10000008L, date(2005, 1, 1), date(2008, 4, 23));
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();
		final MenageCommun menage = ensemble.getMenage();

		// 2007 : le ménage doit être assujetti en raison de son immeuble
		{
			assertEmpty(service.determine(principal, 2007)); // aucun for vaudois
			assertEmpty(service.determine(conjoint, 2007)); // aucun for vaudois

			final List<Assujettissement> assujettissements = service.determine(menage, 2007);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujettissements.get(0));
		}

		// 2008 : le ménage ne doit plus être assujetti, et le principal et le conjoint doivent l'être normalement
		{
			final List<Assujettissement> assujetPrincipal = service.determine(principal, 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = service.determine(conjoint, 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			assertEmpty(service.determine(menage, 2008)); // mariage plus actif
		}
	}	

	@Test
	@Transactional(rollbackFor = Throwable.class)
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@WebScreenshotDoc(description = "[UNIREG-2432] Vérifie qu'un contribuable HC avec immeuble qui se marie n'est plus assujetti l'année de son mariage (cas du contribuable n°101.033.61)")
	public void testDetermineMariageHorsCantonAvecImmeuble() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMariageHCImmeuble(10000006L, 10000007L, 10000008L, date(2009, 11, 1));
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();
		final MenageCommun menage = ensemble.getMenage();

		// 2008 : le principal doit être assujetti en raison de son immeuble
		{
			final List<Assujettissement> assujettissements = service.determine(principal, 2008);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujettissements.get(0));

			assertEmpty(service.determine(conjoint, 2008)); // aucun for vaudois
			assertEmpty(service.determine(menage, 2008)); // mariage pas encore actif
		}

		// 2009 : le principal ne doit plus être assujetti, mais le ménage doit l'être en raison de son immeuble
		{
			assertEmpty(service.determine(principal, 2009)); // immeuble transféré sur le ménage
			assertEmpty(service.determine(conjoint, 2009)); // aucun for vaudois

			final List<Assujettissement> assujettissements = service.determine(menage, 2009);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsCanton(date(2009, 1, 1), date(2009, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujettissements.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@WebScreenshotDoc(description = "[UNIREG-2432] Vérifie qu'un ménage commun HC avec immeuble qui se divorce n'est plus assujetti l'année de son divorce (cas fictif)")
	public void testDetermineDivorceHorsCantonAvecImmeuble() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunDivorceHCImmeuble(10000006L, 10000007L, 10000008L, date(2005, 1, 1), date(2008, 4, 23));
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();
		final MenageCommun menage = ensemble.getMenage();

		// 2007 : le ménage doit être assujetti en raison de son immeuble
		{
			assertEmpty(service.determine(principal, 2007)); // aucun for vaudois
			assertEmpty(service.determine(conjoint, 2007)); // aucun for vaudois

			final List<Assujettissement> assujettissements = service.determine(menage, 2007);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujettissements.get(0));
		}

		// 2008 : le ménage ne doit être assujetti, mais le principal doit l'être en raison de son immeuble
		{
			final List<Assujettissement> assujettissements = service.determine(principal, 2008);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujettissements.get(0));

			assertEmpty(service.determine(conjoint, 2008)); // aucun for vaudois
			assertEmpty(service.determine(menage, 2008)); // mariage plus actif
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@WebScreenshotDoc(description = "[UNIREG-2432] Vérifie qu'un contribuable HS avec immeuble qui se marie n'est plus assujetti l'année de son mariage (cas fictif)")
	public void testDetermineMariageHorsSuisseAvecImmeuble() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMariageHSImmeuble(10000006L, 10000007L, 10000008L, date(2009, 11, 1));
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();
		final MenageCommun menage = ensemble.getMenage();

		// 2008 : le principal doit être assujetti en raison de son immeuble
		{
			final List<Assujettissement> assujettissements = service.determine(principal, 2008);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujettissements.get(0));

			assertEmpty(service.determine(conjoint, 2008)); // aucun for vaudois
			assertEmpty(service.determine(menage, 2008)); // mariage pas encore actif
		}

		// 2009 : le principal ne doit plus être assujetti, mais le ménage doit l'être en raison de son immeuble
		{
			assertEmpty(service.determine(principal, 2009)); // immeuble transféré sur le ménage
			assertEmpty(service.determine(conjoint, 2009)); // aucun for vaudois

			final List<Assujettissement> assujettissements = service.determine(menage, 2009);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujettissements.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	@WebScreenshot(urls = {"/fiscalite/unireg/web/tiers/timeline.do?id=10000006&print=true&title=${methodName}&description=Situation%20de%20Monsieur",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000007&print=true&title=${methodName}&description=Situation%20de%20Madame",
			"/fiscalite/unireg/web/tiers/timeline.do?id=10000008&print=true&title=${methodName}&description=Situation%20du%20couple"})
	@WebScreenshotDoc(description = "[UNIREG-2432] Vérifie qu'un ménage commun HS avec immeuble qui se divorce n'est plus assujetti l'année de son divorce (cas fictif)")
	public void testDetermineDivorceHorsSuisseAvecImmeuble() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunDivorceHSImmeuble(10000006L, 10000007L, 10000008L, date(2005, 1, 1), date(2008, 4, 23));
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();
		final MenageCommun menage = ensemble.getMenage();

		// 2007 : le ménage doit être assujetti en raison de son immeuble
		{
			assertEmpty(service.determine(principal, 2007)); // aucun for vaudois
			assertEmpty(service.determine(conjoint, 2007)); // aucun for vaudois

			final List<Assujettissement> assujettissements = service.determine(menage, 2007);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsSuisse(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujettissements.get(0));
		}

		// 2008 : le ménage ne doit être assujetti, mais le principal doit l'être en raison de son immeuble
		{
			final List<Assujettissement> assujettissements = service.determine(principal, 2008);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujettissements.get(0));

			assertEmpty(service.determine(conjoint, 2008)); // aucun for vaudois
			assertEmpty(service.determine(menage, 2008)); // mariage plus actif
		}
	}
	
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000018&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonDansLAnnee() throws Exception {

		final Contribuable paul = createDepartHorsCanton(10000018L, date(2008, 6, 30));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2008
		{
			assertEmpty(service.determine(paul, 2008));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000019&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonAu31Decembre() throws Exception {

		final Contribuable paul = createDepartHorsCanton(10000019L, date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size()); // un départ-hc au 31 décembre correspond bien à une fin d'assujettissement (cas limite, il est vrai)
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000020&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonDansLAnneeAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsCantonAvecImmeuble(10000020L, date(2008, 6, 30));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
			assertHorsCanton(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HC, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000021&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonAu31DecembreAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsCantonAvecImmeuble(10000021L, date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2009, 1, 1), date(2009, 12, 31), MotifFor.DEPART_HC, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
			assertHorsCanton(date(2009, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HC, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000022&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonEtVenteImmeubleDansLAnnee() throws Exception {

		final Contribuable paul = createDepartHorsCantonEtVenteImmeuble(10000022L, date(2008, 6, 30), date(2008, 9, 30));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2008 (départ puis vente)
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// La période d'assujettissement en raison d'un rattachement économique s'étend à toute l'année (art. 8 al. 6 LI).
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, MotifFor.VENTE_IMMOBILIER, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000023&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierPur() throws Exception {

		final Contribuable paul = createDepartHorsCantonSourcierPur(10000023L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// fractionnement de l'assujettissement
			assertSourcierPur(date(2008, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2008, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000024&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] pas de fractionnement en 2008 car le contribuable reste assujetti toute l'année à raison de son for secondaire (immeuble ou activité " +
			"indépendante)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierMixte137Al1() throws Exception {

		final Contribuable paul = createDepartHorsCantonSourcierMixte137Al1_Invalide(10000024L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] pas de fractionnement dans ce cas-là car le contribuable reste assujetti toute l'année à raison de son for secondaire (immeuble ou activité indépendante).
			assertSourcierMixteArt137Al1(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixteArt137Al1(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierMixteArt137Al1(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000025&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] fractionnement de l'assujettissement en 2008 (mais pas d'arrondi à la fin de mois)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierMixte137Al2() throws Exception {

		final Contribuable paul = createDepartHorsCantonSourcierMixte137Al2(10000025L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// [UNIREG-1742] fractionnement de l'assujettissement (mais pas d'arrondi à la fin de mois)
			assertSourcierMixteArt137Al2(date(2008, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2008, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixteArt137Al2(date(2002, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000026&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonSourcierPur() throws Exception {

		final Contribuable paul = createArriveeHorsCantonSourcierPur(10000026L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// fractionnement de l'assujettissement
			assertSourcierPur(date(2008, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierPur(date(2008, 9, 25), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierPur(date(2008, 9, 25), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=11000026&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonDansLAnnee() throws Exception {

		final Contribuable paul = createArriveeHorsCanton(11000026L, date(2008, 9, 25));

		// 2007
		{
			assertEmpty(service.determine(paul, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000027&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonAvecImmeubleDansLAnnee() throws Exception {

		final Contribuable paul = createArriveeHorsCantonAvecImmeuble(10000027L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.ARRIVEE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsCanton(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.ARRIVEE_HC, list.get(0));
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000028&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonAu1erJanvier() throws Exception {

		final Contribuable paul = createArriveeHorsCanton(10000028L, date(2008, 1, 1));

		// 2007
		{
			assertEmpty(service.determine(paul, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000029&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] pas de fractionnement en 2008 dans ce cas-là car le contribuable reste assujetti toute l'année à raison de son for secondaire (immeuble " +
			"ou activité indépendante).")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonSourcierMixte137Al1() throws Exception {

		final Contribuable paul = createArriveeHorsCantonSourcierMixte137Al1_Invalide(10000029L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] pas de fractionnement à la date d'arrivée dans ce cas-là car le contribuable reste assujetti toute l'année à raison de son for secondaire (immeuble ou activité indépendante).
			assertSourcierMixteArt137Al1(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixteArt137Al1(date(2002, 1, 1), date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixteArt137Al1(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000030&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] fractionnement de l'assujettissement (mais pas d'arrondi à la fin de mois)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonSourcierMixte137Al2() throws Exception {

		final Contribuable paul = createArriveeHorsCantonSourcierMixte137Al2(10000030L, date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// [UNIREG-1742] fractionnement de l'assujettissement (mais pas d'arrondi à la fin de mois)
			assertSourcierPur(date(2008, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixteArt137Al2(date(2008, 9, 25), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixteArt137Al2(date(2008, 9, 25), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000031&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDansLAnnee() throws Exception {

		final Contribuable paul = createDepartHorsSuisse(10000031L, date(2008, 6, 30));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire pendant son séjour en suisse, et non-assujetti hors-Suisse
			assertOrdinaire(date(2008, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseAu31Decembre() throws Exception {

		final Contribuable paul = createDepartHorsSuisse(10000032L, date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire pendant son séjour en suisse, et non-assujetti hors-Suisse
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000033&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDansLAnneeAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsSuisseAvecImmeuble(10000033L, date(2008, 6, 30));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// ordinaire pendant son séjour en Suisse
			assertOrdinaire(date(2008, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
			// hors-Suisse le reste de l'année
			assertHorsSuisse(date(2008, 7, 1), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2008, 7, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000034&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseAu31DecembreAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsSuisseAvecImmeuble(10000034L, date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2009, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}
	
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=11000034&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner l'assujettissement en cours de période (car le rattachement économique n'est pas " +
			"interrompu)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecImmeuble() throws Exception {

		final Contribuable ctb = createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(11000034L, date(2008, 6, 30));

		// 2007 (hors-canton)
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2008 (hors-canton -> hors-Suisse)
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner l'assujettissement en cours de période (car le rattachement économique n'est pas interrompu)
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2009 (hors-Suisse)
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(ctb, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsCanton(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000035&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "(départ HS et arrivée HC dans l'année -> pas moyen de connaître la date d'arrivée de HS dans l'autre canton, on prend toute la période restante par défaut)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseEtArriveeDeHorsCantonDansLAnnee() throws Exception {

		final RegDate dateDepart = RegDate.get(2007, 3, 15);
		final RegDate dateArrivee = RegDate.get(2007, 10, 1);
		final Contribuable ctb = createDepartHorsSuisseEtArriveeDeHorsCanton(10000035L, dateDepart, dateArrivee);

		// 2006
		{
			final List<Assujettissement> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007 (départ HS et arrivée HC dans l'année -> pas moyen de connaître la date d'arrivée de HS dans l'autre canton, on prend toute la période restante par défaut)
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2007, 1, 1), dateDepart, null, MotifFor.DEPART_HS, list.get(0));
			assertOrdinaire(dateDepart.getOneDayAfter(), date(2007, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000036&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] le départ hors-Suisse depuis hors-canton en 2008 ne doit pas fractionner l'assujettissement en cours de période (car le rattachement économique " +
			"n'est pas interrompu)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante() throws Exception {

		final Contribuable ctb = createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(10000036L, date(2008, 6, 30));

		// 2007 (hors-canton)
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2008 (hors-canton -> hors-Suisse)
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner l'assujettissement en cours de période (car le rattachement économique n'est pas interrompu)
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2009 (hors-Suisse)
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(ctb, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsCanton(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	/**
	 * [UNIREG-1327] Vérifie que l'assujettissement d'un HS qui vend son immeuble ne s'étend pas au delà de la date de vente.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000037&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1327] Vérifie que l'assujettissement d'un HS qui vend son immeuble ne s'étend pas au delà de la date de vente.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleContribuableHorsSuisse() throws Exception {

		final RegDate dateAchat = date(2000, 7, 1);
		final RegDate dateVente = date(2007, 5, 30);
		final Contribuable paul = createHorsSuisseAvecAchatEtVenteImmeuble(10000037L, dateAchat, dateVente);

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse jusqu'à la date de la vente de l'immeuble
			assertHorsSuisse(date(2007, 1, 1), dateVente, null, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertEmpty(list);
		}

		// 2000-2008
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(dateAchat, dateVente, MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000038&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseAvecImmeuble() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final Contribuable ctb = createArriveeHorsSuisseAvecImmeuble(10000038L, dateArrivee);

		// 2006
		{
			final List<Assujettissement> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse jusqu'à la date d'arrivée
			assertHorsSuisse(date(2007, 1, 1), dateArrivee.getOneDayBefore(), null, MotifFor.ARRIVEE_HS, list.get(0));
			// ordinaire depuis l'arrivée
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * Version spéciale avec motif de fermeture du fors HS nul.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000039&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Version spéciale avec motif de fermeture du fors HS nul.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseAvecImmeubleEtMotifFermetureNul() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final Contribuable ctb = createArriveeHorsSuisseAvecImmeuble(10000039L, dateArrivee);
		
		final ForFiscalPrincipal ffp0 = (ForFiscalPrincipal) ctb.getForsFiscauxSorted().get(0);
		ffp0.setMotifFermeture(null);

		// 2006
		{
			final List<Assujettissement> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse jusqu'à la date d'arrivée
			// TODO (msi) le motif de fermeture devrait être MotifFor.ARRIVEE_HS
			assertHorsSuisse(date(2007, 1, 1), dateArrivee.getOneDayBefore(), null, null, list.get(0));
			// ordinaire depuis l'arrivée
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * [UNIREG-2759] Vérifie qu'un contribuable qui arrive de HS avec un immeuble puis part hors-canton la même année est calculé comme assujetti hors-canton toute l'année.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000039&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(description = "[UNIREG-2759] Vérifie qu'un contribuable qui arrive de HS avec un immeuble puis part hors-canton la même année est calculé comme assujetti hors-canton toute l'année.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtDepartHorsCantonDansLAnneeAvecImmeuble() throws Exception {

		final RegDate dateArriveeHS = date(2007, 3, 1);
		final RegDate dateDepartHC = date(2007, 8, 4);
		final Contribuable ctb = createArriveeHorsSuisseEtDepartHCAvecImmeuble(10000039L, dateArriveeHS, dateDepartHC);

		// 2006
		{
			final List<Assujettissement> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.ARRIVEE_HS, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-2759] hors-canton toute l'année car son court passage comme vaudois ordinaire ne compte pas (règle des déménagements entre cantons) et
			// que d'un point de vue de l'assujettissement pour motif de rattachement économique, l'arrivée hors-Suisse ne compte pas comme motif de fractionnement.
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), MotifFor.DEPART_HC, null, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-canton toute l'année
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * [UNIREG-2759] Vérifie qu'un contribuable qui arrive de HC avec un immeuble puis part hors-Suisse la même année voit bien son assujettissement fractionné à la date du départ (situation inverse
	 * mais non-symétrique de testDetermineArriveeHorsSuisseEtDepartHorsCantonDansLAnneeAvecImmeuble).
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000039&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-2759] Vérifie qu'un contribuable qui arrive de HC avec un immeuble puis part hors-Suisse la même année voit bien son assujettissement fractionné à " +
			"la date du départ (situation inverse mais non-symétrique de testDetermineArriveeHorsSuisseEtDepartHorsCantonDansLAnneeAvecImmeuble).")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonEtDepartHorsSuisseDansLAnneeAvecImmeuble() throws Exception {

		final RegDate dateArriveeHC = date(2007, 3, 1);
		final RegDate dateDepartHS = date(2007, 8, 4);
		final Contribuable ctb = createArriveeHorsCantonEtDepartHSAvecImmeuble(10000039L, dateArriveeHC, dateDepartHS);

		// 2006
		{
			final List<Assujettissement> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-canton toute l'année
			assertHorsCanton(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.ARRIVEE_HC, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// [UNIREG-2759] vaudois ordinaire jusqu'à son départ, puis hors-Suisse.
			assertOrdinaire(date(2007, 1, 1), dateDepartHS, MotifFor.ARRIVEE_HC, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(dateDepartHS.getOneDayAfter(), date(2007, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * [UNIREG-3261] Cas du ctb n°10546107. Vérifie qu'un contribuable sourcier pur qui arrive de hors-Suisse et part hors-canton la même année est bien assujetti comme sourcier pur pendant toute la durée de
	 * sa présence sur sol vaudois (situation similaire mais avec comportement différent du test testDetermineArriveeHorsSuisseEtDepartHorsCantonDansLAnneeAvecImmeuble parce que le contribuable est
	 * sourcier pur).
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10546107&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(description = "Cas du ctb n°10546107. Vérifie qu'un contribuable sourcier pur qui arrive de hors-Suisse et part hors-canton la même année est bien assujetti comme sourcier " +
			"pur pendant toute la durée de sa présence sur sol vaudois (situation similaire mais avec comportement différent du test " +
			"testDetermineArriveeHorsSuisseEtDepartHorsCantonDansLAnneeAvecImmeuble parce que le contribuable est sourcier pur)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtDepartHorsCantonDansLAnneeSourcierPur() throws Exception {

		final RegDate dateArriveeHS = date(2010, 9, 6);
		final RegDate dateDepartHC = date(2010, 10, 18);
		final Contribuable ctb = createArriveeHorsSuisseEtDepartHCSourcier(10546107L, dateArriveeHS, dateDepartHC);

		// 2009
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pure hors-Suisse toute l'année
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2010
		{
			final List<Assujettissement> list = service.determine(ctb, 2010);
			assertNotNull(list);
			assertEquals(3, list.size());
			// sourcier pur hors-Suisse jusqu'à l'arrivee
			assertSourcierPur(date(2010, 1, 1), date(2010, 9, 5), null, MotifFor.ARRIVEE_HS, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			// sourcier pur vaudois pendant les mois de septembre et octobre 2010
			assertSourcierPur(date(2010, 9, 6), date(2010, 10, 18), MotifFor.ARRIVEE_HS, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
			// sourcier pur hors-canton après le départ
			assertSourcierPur(date(2010, 10, 19), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(2));
		}

		// 2011
		{
			final List<Assujettissement> list = service.determine(ctb, 2011);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pure hors-canton toute l'année
			assertSourcierPur(date(2011, 1, 1), date(2011, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}
	}

	/**
	 * [UNIREG-3261] Cas du ctb n°10558415. En cas de départ et d'arrivée hors-Suisse la même année avec un motif d'arrivée faux (hors-canton) d'un sourcier pur. L'algorithme doit détecter
	 * l'erreur dans le motif et quand même fractionner l'assujettissement à la date d'arrivée. Autrement, les périodes d'assujettissement source se chevauchent.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10558415&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(description = "Cas du ctb n°10558415. En cas de départ et d'arrivée hors-Suisse la même année avec un motif d'arrivée faux (hors-canton) d'un sourcier pur. " +
			"L'algorithme doit détecter l'erreur dans le motif et quand même fractionner l'assujettissement à la date d'arrivée.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseEtArriveeHorsSuisseDansLAnneeMaisAvecMotifArriveHorsCantonSourcierPur() throws Exception {

		final RegDate dateDepartHS = date(2009, 3, 11);
		final RegDate dateArriveeHS = date(2009, 9, 1);
		final Contribuable ctb = createDepartHorsSuisseEtArriveeHorsSuisseDansLAnneeMaisAvecMotifArriveeHorsCantonSourcierPur(10558415L, dateDepartHS, dateArriveeHS);

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pur VD toute l'année
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(3, list.size());
			// sourcier pur VD jusqu'à son départ HS
			assertSourcierPur(date(2009, 1, 1), dateDepartHS, null, MotifFor.DEPART_HS, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			// sourcier pur HS durant son séjour HS
			assertSourcierPur(dateDepartHS.getOneDayAfter(), dateArriveeHS.getOneDayBefore(), MotifFor.DEPART_HS, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.PAYS_HS, list.get(1));
			// et de sourcier pur VD dès son retour
			assertSourcierPur(dateArriveeHS, date(2009, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(2));
		}

		// 2010
		{
			final List<Assujettissement> list = service.determine(ctb, 2010);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pur VD toute l'année
			assertSourcierPur(date(2010, 1, 1), date(2010, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * [UNIREG-3261] En cas d'arrivée et de départ hors-Suisse la même année avec un motif de départ faux (hors-canton) d'un sourcier pur. L'algorithme doit détecter
	 * l'erreur dans le motif et quand même fractionner l'assujettissement à la date de départ. Autrement, les périodes d'assujettissement source se chevauchent (cas inverse du test
	 * testDetermineDepartHorsSuisseEtArriveeHorsSuisseDansLAnneeMaisAvecMotifArriveHorsCantonSourcierPur).
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10558415&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(description = "En cas d'arrivée et de départ hors-Suisse la même année avec un motif de départ faux (hors-canton) d'un sourcier pur. L'algorithme doit détecter " +
			"l'erreur dans le motif et quand même fractionner l'assujettissement à la date de départ. Autrement, les périodes d'assujettissement source se chevauchent (cas inverse du test " +
			"testDetermineDepartHorsSuisseEtArriveeHorsSuisseDansLAnneeMaisAvecMotifArriveHorsCantonSourcierPur)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtDepartHorsSuisseDansLAnneeMaisAvecMotifDepartHorsCantonSourcierPur() throws Exception {

		final RegDate dateArriveeHS = date(2010, 9, 6);
		final RegDate dateDepartHS = date(2010, 10, 18);
		final Contribuable ctb = createArriveeHorsSuisseEtDepartHorsSuisseDansLAnneeMaisAvecMotifDepartHorsCantonSourcierPur(10558415L, dateArriveeHS, dateDepartHS);

		// 2009
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pur HS toute l'année
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2010
		{
			final List<Assujettissement> list = service.determine(ctb, 2010);
			assertNotNull(list);
			assertEquals(3, list.size());
			// sourcier pur HS jusqu'à l'arrivée
			assertSourcierPur(date(2010, 1, 1), dateArriveeHS.getOneDayBefore(), null, MotifFor.ARRIVEE_HS, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			// sourcier pur VD le temps du passage en Suisse
			assertSourcierPur(dateArriveeHS, dateDepartHS, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
			// sourcier pur HS dès le départ
			assertSourcierPur(dateDepartHS.getOneDayAfter(), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.PAYS_HS, list.get(2));
		}

		// 2011
		{
			final List<Assujettissement> list = service.determine(ctb, 2011);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pur HS toute l'année
			assertSourcierPur(date(2011, 1, 1), date(2011, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}
	}

	/**
	 * [UNIREG-1327] Vérifie que l'assujettissement d'un contribuable HS qui possède un immeuble, arrive de HS et vend son immeuble dans la
	 * même année est bien fractionné à la date d'arrivée HS.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000040&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1327] Vérifie que l'assujettissement d'un contribuable HS qui possède un immeuble, arrive de HS et vend son immeuble dans la même année est bien " +
			"fractionné à la date d'arrivée HS")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtVenteImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateVente = date(2007, 5, 30);
		final Contribuable paul = createArriveeHorsSuisseEtVenteImmeuble(10000040L, dateArrivee, dateVente);

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse jusqu'à la date d'arrivée
			assertHorsSuisse(date(2007, 1, 1), dateArrivee.getOneDayBefore(), null, MotifFor.ARRIVEE_HS, list.get(0));
			// ordinaire depuis l'arrivée
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}

		// 2000-2008
		{
			final List<Assujettissement> list = service.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsSuisse(date(2000, 1, 1), dateArrivee.getOneDayBefore(), MotifFor.ACHAT_IMMOBILIER, MotifFor.ARRIVEE_HS, list.get(0));
			assertOrdinaire(dateArrivee, date(2008, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000041&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveHorsSuisseEtDemenagementVaudoisDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateDemenagement = date(2007, 7, 1);
		final Contribuable ctb = createArriveHorsSuisseEtDemenagementVaudoisDansLAnnee(10000041L, dateArrivee, dateDemenagement);

		// 2006
		{
			assertEmpty(service.determine(ctb, 2006));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble entre-deux.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000042&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble entre-deux.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseAchatImmeubleEtDepartHorsSuisseDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateAchat = date(2007, 5, 30);
		final RegDate dateDepart = date(2007, 12, 8);
		final Contribuable paul = createArriveeHSAchatImmeubleEtDepartHS(10000042L, dateArrivee, dateAchat, dateDepart);

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertEmpty(list);
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse non-assujetti avant l'arrivée, ordinaire ensuite
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			// hors-Suisse mais assujetti après son départ
			assertHorsSuisse(dateDepart.getOneDayAfter(), date(2007, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}

		// 2000-2008
		{
			final List<Assujettissement> list = service.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(dateDepart.getOneDayAfter(), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble après son départ. Il doit
	 * y avoir deux assujettissements distincts : un pour sa présence en Suisse, et un autre pour son immeuble acheté plus tard.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000043&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble après son départ. Il doit y avoir deux " +
			"assujettissements distincts : un pour sa présence en Suisse, et un autre pour son immeuble acheté plus tard.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtDepartHorsSuissePuisAchatImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 2, 1);
		final RegDate dateDepart = date(2007, 7, 30);
		final RegDate dateAchat = date(2007, 10, 8);
		final Contribuable paul = createArriveeHSDepartHSPuisAchatImmeuble(10000043L, dateArrivee, dateDepart, dateAchat);

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertEmpty(list);
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// assujetti comme ordinaire pendant son passage en suisse
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			// assujetti comme hors-Suisse suite à l'achat de son immeuble
			assertHorsSuisse(dateAchat, date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}

		// 2000-2008
		{
			final List<Assujettissement> list = service.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(dateAchat, date(2008, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000044&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePassageRoleSourceAOrdinaire() throws Exception {

		final Contribuable paul = createPassageRoleSourceAOrdinaire(10000044L, date(2008, 2, 12));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// sourcier pure les deux premiers mois (=> arrondi au mois)
			assertSourcierPur(date(2008, 1, 1), date(2008, 2, 29), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			// ordinaire le reste de l'année
			assertOrdinaire(date(2008, 3, 1), date(2008, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(1));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 2, 29), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertOrdinaire(date(2008, 3, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000044&print=true&title=${methodName}")
	@WebScreenshotDoc(description = "Cas limite du passage sourcier pure à ordinaire à la mi-décembre: " +
			"l'assujettissement sourcier pur est étendu jusqu'à la fin de l'année et l'assujettissement ordinaire ne commence qu'au début de l'année suivante.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePassageRoleSourceAOrdinaireCasLimiteFinDAnnee() throws Exception {

		final Contribuable paul = createPassageRoleSourceAOrdinaire(10000044L, date(2008, 12, 12));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// sourcier pure jusqu'à mi-décembre -> l'assujettissement sourcier pur est étendu jusqu'à la fin de l'année
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertOrdinaire(date(2009, 1, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(1));
		}
	}

	/**
	 * [UNIREG-2444] Cas du contribuble n°106.846.77
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10684677&print=true&title=${methodName}")
	@WebScreenshotDoc(description = "Cas du contribuable ce contribuable ne possède qu'un seul for fiscal principal qui commence donc le 12.12.2008 avec le motif d'obtention de permis C. " +
			"Cela laisse supposer qu'il possédait précédemment un permis B et qu'il était donc sourcier, mais il n'y a aucune trace de cela. " +
			"Dans ce cas, on calcule l'assujettissement comme s'il existait un for source valide du début de l'année à la veille de l'obtention du permis.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePassageRoleSourceAOrdinaireImplicite() throws Exception {

		final Contribuable paul = createPassageRoleSourceAOrdinaireImplicite(10684677L, date(2008, 12, 12));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// passage du rôle source à ordinaire implicite -> assujetti en tant que sourcier sur toute l'année à cause de l'arrondi en fin de mois
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), MotifFor.INDETERMINE, MotifFor.PERMIS_C_SUISSE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), MotifFor.PERMIS_C_SUISSE, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), MotifFor.INDETERMINE, MotifFor.PERMIS_C_SUISSE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertOrdinaire(date(2009, 1, 1), date(2010, 12, 31), MotifFor.PERMIS_C_SUISSE, null, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000045&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierPureHorsCanton() throws Exception {

		final Contribuable paul = createSourcierPureHorsCanton(10000045L);

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2010, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000046&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierPureHorsSuisse() throws Exception {

		final Contribuable paul = createSourcierPureHorsSuisse(10000046L);

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2010, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000047&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Note: le passage de sourcier pure à sourcier mixte ne provoque pas de fractionnement de l'assujettissement, la validité de l'assujettissement sourcier mixte " +
			"débute simplement le 1er janvier")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierMixte137Al1HorsCanton() throws Exception {

		final RegDate achatImmeuble = date(2007, 7, 1);
		final Contribuable paul = createSourcierMixte137Al1HorsCanton_Invalide(10000047L, achatImmeuble);

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2007, 1, 1), date(2007, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2006, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			/*
			 * Note: le passage de sourcier pure à sourcier mixte ne provoque pas de fractionnement de l'assujettissement, la validité de
			 * l'assujettissement sourcier mixte débute simplement le 1er janvier
			 */
			assertSourcierMixteArt137Al1(date(2007, 1, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000048&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierMixte137Al1HorsSuisse() throws Exception {

		final RegDate dateChangement = date(2007, 7, 1);
		final Contribuable paul = createSourcierMixte137Al1HorsSuisse_Invalide(10000048L, dateChangement);

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2007
		{
			// passage sourcier pure à sourcier mixte -> fractionnement de l'assujettissement
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// TODO (msi) voir s'il est possible d'exposer le motif d'ouverture du for secondaire (qui est plus précis que celui du for principal)
			assertSourcierPur(date(2007, 1, 1), dateChangement.getOneDayBefore(), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			assertSourcierMixteArt137Al1(dateChangement, date(2007, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.PAYS_HS, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), dateChangement.getOneDayBefore(), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			assertSourcierMixteArt137Al1(dateChangement, date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.PAYS_HS, list.get(1));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000049&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierMixte137Al2() throws Exception {

		final Contribuable paul = createSourcierPassageMixte137Al2(10000049L, date(2005, 1, 1));

		// 2008
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2004, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierMixteArt137Al2(date(2005, 1, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	/**
	 * Teste le cas limite où le passage du mode d'imposition ordinaire -> sourcier tombe au milieu du premier mois.
	 * <p>
	 * Selon les règles en vigueur, le passage source -> ordinaire doit tomber au fin de mois: les périodes d'assujettissement doivent donc
	 * être ajustées en conséquence. Et il s'agit donc d'un cas particulier parce qu'en avançant le début d'assujettissement source du 16
	 * janvier au 1 janvier, la première période d'assujettissement ordinaire (du 1er janvier au 15) est écrasée.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000050&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Teste le cas limite où le passage du mode d'imposition ordinaire -> sourcier tombe au milieu du premier mois. Selon les règles en vigueur, le passage source -> " +
			"ordinaire doit tomber au fin de mois: les périodes d'assujettissement doivent donc être ajustées en conséquence. Et il s'agit donc d'un cas particulier parce qu'en avançant le début " +
			"d'assujettissement source du 16 janvier au 1 janvier, la première période d'assujettissement ordinaire (du 1er janvier au 15) est écrasée.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineOrdinairePuisSourcierCasLimite() throws Exception {

		final Contribuable paul = createOrdinairePuisSourcierCasLimite(10000050L);

		// 2005 (ordinaire)
		{
			final List<Assujettissement> list = service.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2005, 1, 1), date(2005, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, list.get(0));
		}

		// 2006 (passage à la source pure le 16 janvier)
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size()); // <--- une seule période (voir commentaire de la méthode)
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Teste le cas limite où le passage du mode d'imposition sourcier -> ordinaire tombe au milieu du dernier mois.
	 * <p>
	 * Selon les règles en vigueur, le passage source -> ordinaire doit tomber au fin de mois: les périodes d'assujettissement doivent donc
	 * être ajustées en conséquence. Et il s'agit donc d'un cas particulier parce qu'en poussant la fin d'assujettissement source du 16
	 * décembre au 31 décembre, la seconde période d'assujettissement ordinaire (du 17 décembre au 31) est écrasée.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000051&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Teste le cas limite où le passage du mode d'imposition sourcier -> ordinaire tombe au milieu du dernier mois. Selon les règles en vigueur, le passage source -> " +
			"ordinaire doit tomber au fin de mois: les périodes d'assujettissement doivent donc être ajustées en conséquence. Et il s'agit donc d'un cas particulier parce qu'en poussant la fin " +
			"d'assujettissement source du 16 décembre au 31 décembre, la seconde période d'assujettissement ordinaire (du 17 décembre au 31) est écrasée.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierPuisOrdinaireCasLimite() throws Exception {

		final Contribuable paul = createSourcierPuisOrdinaireCasLimite(10000051L);

		// 2005
		{
			final List<Assujettissement> list = service.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2005, 1, 1), date(2005, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2006 (passage au rôle ordinaire le 17 décembre)
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size()); // <--- une seule période (voir commentaire de la méthode)
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000052&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1976] le fait de posséder un immeuble en suisse ne fait plus basculer le diplomate dans la catégorie hors-Suisse: il reste diplomate suisse (mais il recevra une déclaration d'impôt ordinaire)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDiplomateSuisseAvecImmeuble() throws Exception {

		final Contribuable paul = createDiplomateAvecImmeuble(10000052L, date(2000, 1, 1), date(2001, 6, 13));

		// 1999
		{
			final List<Assujettissement> list = service.determine(paul, 1999);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(1999, 1, 1), date(1999, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2000 (nomination comme diplomate suisse basé à l'étanger)
		{
			final List<Assujettissement> list = service.determine(paul, 2000);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertDiplomateSuisse(date(2000, 1, 1), date(2000, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2001 (achat d'un immeuble au 13 juin)
		{
			final List<Assujettissement> list = service.determine(paul, 2001);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1976] le fait de posséder un immeuble en suisse ne fait plus basculer le diplomate dans la catégorie hors-Suisse: il reste diplomate suisse.
			assertDiplomateSuisse(date(2001, 1, 1), date(2001, 12, 31), null, null, list.get(0));
		}

		// 1999-2010
		{
			List<Assujettissement> list = service.determine(paul, RANGE_1999_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(1999, 1, 1), date(1999, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertDiplomateSuisse(date(2000, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	/**
	 * [UNIREG-1390] Vérifie qu'il est possible de déterminer l'assujettissement d'un hors-Suisse qui vend son immeuble et dont le for
	 * principal est fermé sans motif (cas du ctb n°807.110.03).
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000053&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1390] Vérifie qu'il est possible de déterminer l'assujettissement d'un hors-Suisse qui vend son immeuble et dont le for principal est fermé sans motif (cas du ctb n°807.110.03).")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineHorsSuisseForPrincipalFermeSansMotif() throws Exception {

		final RegDate dateVente = date(2009, 3, 24);
		final Contribuable ctb = createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(10000053L, dateVente);

		final List<Assujettissement> list = service.determine(ctb, 2009);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertHorsSuisse(date(2009, 1, 1), dateVente, null, MotifFor.VENTE_IMMOBILIER, list.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000054&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineHorsCantonAvecImmeuble() throws Exception {

		final RegDate dateAchat = date(2008, 4, 21);
		final Contribuable ctb = createHorsCantonAvecImmeuble(10000054L, dateAchat);

		// 2007
		{
			assertEmpty(service.determine(ctb, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2010
		{
			final List<Assujettissement> list = service.determine(ctb, 2010);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2010, 1, 1), date(2010, 12, 31), null, null, list.get(0));
		}

		// 2011
		{
			final List<Assujettissement> list = service.determine(ctb, 2011);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2011, 1, 1), date(2011, 12, 31), null, null, list.get(0));
		}

		// 2007-2011
		{
			List<Assujettissement> list = service.determine(ctb, new DateRangeHelper.Range(date(2007, 1, 1), date(2011, 12, 31)), true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2011, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(0));
		}
	}

	/**
	 * [UNIREG-1742] Vérifie que l'assujettissement d'un contribuable hors-Suisse débute/arrête bien à l'achat/vente du premier/dernier immeuble. Dans le cas d'achats et de ventes de plusieurs immeubles
	 * (sans chevauchement) dans le même année, les périodes sont donc fractionnées.
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000055&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] Vérifie que l'assujettissement d'un contribuable hors-Suisse débute/arrête bien à l'achat/vente du premier/dernier immeuble. Dans le cas d'achats " +
			"et de ventes de plusieurs immeubles (sans chevauchement) dans le même année, les périodes sont donc fractionnées.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineAchatsEtVentesMultipleHorsSuisse() throws Exception {

		final Range immeuble1 = new Range(date(2008, 1, 15), date(2008, 3, 30));
		final Range immeuble2 = new Range(date(2008, 6, 2), date(2008, 11, 26));
		final Contribuable ctb = createHorsSuisseAvecAchatsEtVentesImmeubles(10000055L, immeuble1, immeuble2);

		// 2007
		{
			assertEmpty(service.determine(ctb, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsSuisse(immeuble1.getDateDebut(), immeuble1.getDateFin(), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, list.get(0));
			assertHorsSuisse(immeuble2.getDateDebut(), immeuble2.getDateFin(), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, list.get(1));
		}

		// 2009
		{
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	/**
	 * [UNIREG-1742] Vérifie que les périodes d'un contribuable hors-Suisse sourcier sont bien fractionnées en cas d'achat d'un immeuble (passage pur -> mixte, et vice-versa).
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000056&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-1742] Vérifie que les périodes d'un contribuable hors-Suisse sourcier sont bien fractionnées en cas d'achat d'un immeuble (passage pur -> mixte, et " +
			"vice-versa).")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineAchatEtVenteImmeubleHorsSuisseSourcier() throws Exception {

		final RegDate dateAchat = date(2008, 1, 15);
		final RegDate dateVente = date(2008, 3, 30);
		final Contribuable ctb = createHorsSuisseSourcierAvecAchatEtVenteImmeuble_Invalide(10000056L, dateAchat, dateVente);

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertSourcierPur(date(2008, 1, 1), dateAchat.getOneDayBefore(), null, MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			assertSourcierMixteArt137Al1(dateAchat, dateVente, MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, TypeAutoriteFiscale.PAYS_HS, list.get(1));
			assertSourcierPur(dateVente.getOneDayAfter(), date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, null, TypeAutoriteFiscale.PAYS_HS, list.get(2));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000057&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleHorsCanton() throws Exception {

		final RegDate dateVente = date(2008, 9, 30);
		final Contribuable ctb = createVenteImmeubleHorsCanton(10000057L, dateVente);

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (vente de l'immeuble en cours d'année)
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000058&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFinActiviteHorsCanton() throws Exception {

		final RegDate dateFin = date(2008, 9, 30);
		final Contribuable ctb = createFinActiviteHorsCanton(10000058L, dateFin);

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (fin d'activité indépendante en cours d'année)
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.FIN_EXPLOITATION, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000059&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesHorsCantonAvecImmeuble() throws Exception {

		final RegDate dateDeces = date(2008, 10, 26);
		final Contribuable ctb = createDecesHorsCantonAvecImmeuble(10000059L, date(2006, 8, 5), dateDeces);

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (décès en cours d'année)
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), dateDeces, null, MotifFor.VEUVAGE_DECES, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000060&print=true&title=${methodName}")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesHorsCantonActiviteIndependante() throws Exception {

		final RegDate dateDeces = date(2008, 2, 23);
		final Contribuable ctb = createDecesHorsCantonActiviteIndependante(10000060L, date(1990, 4, 13), dateDeces);

		// 2007
		{
			final List<Assujettissement> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (décès en cours d'année)
		{
			final List<Assujettissement> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), dateDeces, null, MotifFor.VEUVAGE_DECES, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	/**
	 * Cas du contribuable n°16109718
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000061&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°16109718")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseAvecImmeubleEtMotifDemanagement() throws Exception {

		final RegDate dateAchat = date(1998, 10, 17);
		final RegDate dateArrivee = date(2003, 1, 1);
		final Contribuable ctb = createArriveeHorsSuisseAvecImmeubleEtMotifDemanagement(10000061L, dateAchat, dateArrivee);

		// 2002
		{
			final List<Assujettissement> list = service.determine(ctb, 2002);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2002, 1, 1), date(2002, 12, 31), null, MotifFor.DEMENAGEMENT_VD, list.get(0));
		}

		// 2003 (arrivée au 1er janvier)
		{
			final List<Assujettissement> list = service.determine(ctb, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2003, 1, 1), date(2003, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(0));
		}

		// 2004
		{
			final List<Assujettissement> list = service.determine(ctb, 2004);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2004, 1, 1), date(2004, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10000171
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000171&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10000171")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineCasCompletementTordu() throws Exception {

		final Contribuable paul = createContribuableSansFor(10000171L);

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2003, 4, 1), MotifFor.INDETERMINE, date(2005, 12, 18), MotifFor.DEPART_HS, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		addForPrincipal(paul, date(2005, 12, 19), MotifFor.DEMENAGEMENT_VD, date(2008, 5, 28), MotifFor.DEMENAGEMENT_VD, MockPays.France);

		fp = addForPrincipal(paul, date(2008, 7, 7), MotifFor.ARRIVEE_HS, date(2008, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		fp = addForPrincipal(paul, date(2008, 8, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		addForSecondaire(paul, date(2008, 7, 7), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2002
		{
			assertEmpty(service.determine(paul, 2002));
		}

		// 2003 (arrivée indéterminée)
		{
			final List<Assujettissement> list = service.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2003, 1, 1), date(2003, 12, 31), MotifFor.INDETERMINE, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2004
		{
			final List<Assujettissement> list = service.determine(paul, 2004);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2004, 1, 1), date(2004, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2005 (départ hors-Suisse)
		{
			final List<Assujettissement> list = service.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2005, 1, 1), date(2005, 12, 18), null, MotifFor.DEPART_HS, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2006
		{
			assertEmpty(service.determine(paul, 2006));
		}

		// 2007
		{
			assertEmpty(service.determine(paul, 2007));
		}

		// 2008 (arrivée de hors-Suisse + achat immobilier)
		{
			final List<Assujettissement> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2008, 7, 7), date(2008, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = service.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10002045
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10002045&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10002045 (le motif d'ouverture du second for principal est incorrect parce que le for immédiatement précédent n'est pas hors-Suisse. " +
			"Dans ce cas-là, il ne doit pas y avoir de fractionnement de l'assujettissement.)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFausseArriveeHorsSuisse() throws Exception {

		final Contribuable paul = createContribuableSansFor(10002045L);

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2001, 1, 1), MotifFor.ARRIVEE_HS, date(2003, 10, 9), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aubonne);
		fp.setModeImposition(ModeImposition.SOURCE);

		// le motif d'ouverture du second for principal est incorrect parce que le for immédiatement précédent n'est pas hors-Suisse.
		// Dans ce cas-là, il ne doit pas y avoir de fractionnement de l'assujettissement.
		fp = addForPrincipal(paul, date(2003, 10, 10), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		// 2000
		{
			assertEmpty(service.determine(paul, 2000));
		}

		// 2001 (arrivée HS)
		{
			final List<Assujettissement> list = service.determine(paul, 2001);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2001, 1, 1), date(2001, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002
		{
			final List<Assujettissement> list = service.determine(paul, 2002);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2002, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2003 (fausse arrivée HS + changement mode d'imposition)
		{
			final List<Assujettissement> list = service.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2003, 1, 1), date(2003, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10003318
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003318&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10003318 (le motif d'ouverture du second for principal est incorrect parce que le for immédiatement précédent n'est pas hors-Suisse. " +
			"Dans ce cas-là, il ne doit pas y avoir de fractionnement de l'assujettissement.)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFausseArriveeHorsSuisse2() throws Exception {

		final Contribuable paul = createContribuableSansFor(10003318L);

		addForPrincipal(paul, date(2002, 5, 31), MotifFor.INDETERMINE, date(2002, 6, 4), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
		// le motif d'ouverture du second for principal est incorrect parce que le for immédiatement précédent n'est pas hors-Suisse.
		// Dans ce cas-là, il ne doit pas y avoir de fractionnement de l'assujettissement.
		addForPrincipal(paul, date(2002, 6, 5), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2007, 7, 1), MotifFor.DEMENAGEMENT_VD, MockPays.Albanie);

		addForSecondaire(paul, date(2002, 6, 5), MotifFor.ACHAT_IMMOBILIER, date(2002, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2001
		{
			assertEmpty(service.determine(paul, 2001));
		}

		// 2002 (achat immeuble puis arrivée HC)
		{
			final List<Assujettissement> list = service.determine(paul, 2002);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2002, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(0));
		}

		// 2003
		{
			final List<Assujettissement> list = service.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2003, 1, 1), date(2003, 12, 31), null, null, list.get(0));
		}

		// 2007 (départ HS)
		{
			final List<Assujettissement> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2008
		{
			assertEmpty(service.determine(paul, 2008));
		}
	}

	/**
	 * Cas du contribuable n°10003348
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003348&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10003348")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierPurePuisMixteSurDepartHSPuisArriveeHSDansLAnneeAvecImmeubleEtMotifsGrandguignolesques() throws Exception {

		final Contribuable paul = createSourcierPurePuisMixteSurDepartHSPuisArriveeHSDansLAnneeAvecImmeubleEtMotifsGrandguignolesques_Invalide();

		// 2002
		{
			assertEmpty(service.determine(paul, 2002));
		}

		// 2003 (arrivée HS sourcier pure + départ HS sourcier mixte 137 al. 1 + arrivée HS sourcier mixte 137 al. 2)
		{
			final List<Assujettissement> list = service.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertSourcierPur(date(2003, 1, 1), date(2003, 5, 27), MotifFor.ARRIVEE_HS, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierMixteArt137Al1(date(2003, 5, 28), date(2003, 8, 30), MotifFor.INDETERMINE, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.PAYS_HS, list.get(1));
			assertSourcierMixteArt137Al2(date(2003, 8, 31), date(2003, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(2));
		}

		// 2004
		{
			final List<Assujettissement> list = service.determine(paul, 2004);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2004, 1, 1), date(2004, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10002080
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10002080&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10002080")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFauxDemenagementVD() throws Exception {

		final Contribuable paul = createFauxDemenagementVD_Invalide();

		// 2002
		{
			assertEmpty(service.determine(paul, 2002));
		}

		// 2003 (achat immeuble)
		{
			final List<Assujettissement> list = service.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al1(date(2003, 1, 1), date(2003, 12, 31), MotifFor.INDETERMINE, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2004 (arrivée hors-canton)
		{
			final List<Assujettissement> list = service.determine(paul, 2004);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixteArt137Al1(date(2004, 1, 1), date(2004, 7, 26), null, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixteArt137Al2(date(2004, 7, 27), date(2004, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}

		// 2005
		{
			final List<Assujettissement> list = service.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixteArt137Al2(date(2005, 1, 1), date(2005, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10004709
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10004709&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10004709 (Dans le cas d'un contribuable avec deux fors principaux vaudois se touchant avec changement au 31 décembre pour " +
			"motif DEPART_HS, on vérifie que le motiff DEPART_HS est bien ignoré)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFauxDepartHS() throws Exception {

		final Contribuable paul = createContribuableSansFor(10004709L);

		// Dans le cas d'un contribuable avec deux fors principaux vaudois se touchant avec changement
		// au 31 décembre pour motif DEPART_HS, on vérifie que le motiff DEPART_HS est bien ignoré
		addForPrincipal(paul, date(2005, 10, 1), MotifFor.ARRIVEE_HC, date(2005, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2006, 1, 1), MotifFor.INDETERMINE, date(2006, 12, 31), MotifFor.DEPART_HC, MockCommune.Cossonay);

		// 2004
		{
			assertEmpty(service.determine(paul, 2004));
		}

		// 2005
		{
			final List<Assujettissement> list = service.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2005, 1, 1), date(2005, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2006
		{
			final List<Assujettissement> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2007
		{
			assertEmpty(service.determine(paul, 2004));
		}

		// 2004-2007
		{
			final List<Assujettissement> list = service.determine(paul);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2005, 1, 1), date(2006, 12, 31), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10008508
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10008508&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10008508")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHSAchatImmeubleEtArriveeHC() throws Exception {

		final Contribuable paul = createContribuableSansFor(10008508L);

		addForPrincipal(paul, date(1995, 11, 2), MotifFor.ARRIVEE_HS, date(1997, 3, 1), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(1997, 3, 3), MotifFor.DEMENAGEMENT_VD, date(2004, 12, 31), MotifFor.DEPART_HC, MockPays.Albanie);
		addForPrincipal(paul, date(2005, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2004, 3, 2), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> list = service.determine(paul);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertOrdinaire(date(1995, 11, 2), date(1997, 3, 1), MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
		assertHorsSuisse(date(2004, 3, 2), date(2004, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.DEPART_HC, list.get(1));
		assertHorsCanton(date(2005, 1, 1), null, MotifFor.ARRIVEE_HS, null, list.get(2));
	}

	/**
	 * Cas du contribuable n°10015452
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10015452&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10015452")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierMixteHorsSuisseAvecImmeuble() throws Exception {

		final Contribuable paul = createSourcierMixteHorsSuisseAvecImmeuble_Invalide();

		final List<Assujettissement> list = service.determine(paul);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertSourcierMixteArt137Al1(date(2004, 5, 6), date(2006, 7, 31), MotifFor.INDETERMINE, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		assertSourcierMixteArt137Al2(date(2006, 8, 1), null, MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
	}

	/**
	 * Cas du contribuable n°10019036
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10019036&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "Cas du contribuable n°10019036")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierHCDepartHSEtAchatImmeuble() throws Exception {

		final Contribuable paul = createContribuableSansFor(10019036L);

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2004, 1, 1), MotifFor.ARRIVEE_HS, date(2004, 6, 14), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2004, 6, 15), MotifFor.DEPART_HC, date(2004, 7, 10), MotifFor.DEPART_HC, MockPays.Danemark);
		addForPrincipal(paul, date(2004, 7, 11), MotifFor.ARRIVEE_HS, date(2004, 7, 11), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
		addForPrincipal(paul, date(2004, 7, 12), MotifFor.ARRIVEE_HC, date(2006, 11, 15), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2006, 11, 16), MotifFor.DEMENAGEMENT_VD, MockPays.Albanie);

		addForSecondaire(paul, date(2004, 6, 15), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> list = service.determine(paul);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertSourcierPur(date(2004, 1, 1), date(2004, 6, 30), MotifFor.ARRIVEE_HS, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		assertOrdinaire(date(2004, 7, 1), date(2006, 11, 15), MotifFor.CHGT_MODE_IMPOSITION, MotifFor.DEPART_HS, list.get(1));
		assertHorsSuisse(date(2006, 11, 16), null, MotifFor.DEMENAGEMENT_VD, null, list.get(2));
	}

	/**
	 * [UNIREG-2155] Cas du contribuable n°10441002
	 */
	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10441002&print=true&title=${methodName}&description=${docDescription}")	
	@WebScreenshotDoc(description = "[UNIREG-2155] Cas du contribuable n°10441002")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHSSourcierEtArriveeHSOrdinaireMemeMois() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10441002L);

		ForFiscalPrincipal fp = addForPrincipal(ctb, date(2003, 1, 1), MotifFor.ARRIVEE_HS, date(2009, 8, 13), MotifFor.DEPART_HS, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, date(2009, 8, 14), MotifFor.DEPART_HS, date(2009, 8, 18), MotifFor.ARRIVEE_HS, MockPays.Colombie);
		addForPrincipal(ctb, date(2009, 8, 19), MotifFor.ARRIVEE_HS, date(2009, 12, 13), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(ctb, date(2009, 12, 14), MotifFor.DEPART_HS, MockPays.EtatsUnis);

		final List<Assujettissement> list = service.determine(ctb);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertSourcierPur(date(2003, 1, 1), date(2009, 8, 13), MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		assertOrdinaire(date(2009, 8, 19), date(2009, 12, 13), MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(1));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=17907715&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(description = "[UNIREG-2559] Cas du contribuable n°17907715 (vérifie que l'algo ne crashe pas en cas d'achat d'immeuble et mariage dans la même année)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineAchatImmeubleEtMariageDansLAnnee() throws Exception {

		final Contribuable ctb = createContribuableSansFor(17907715L);
		addForPrincipal(ctb, date(1993, 1, 6), MotifFor.INDETERMINE, date(2008, 2, 29), MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(ctb, date(2008, 3, 1), MotifFor.DEMENAGEMENT_VD, date(2009, 8, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2009, 8, 31), MotifFor.ACHAT_IMMOBILIER, date(2009, 8, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Renens.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> list = service.determine(ctb);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(1993, 1, 1), date(2007, 12, 31), MotifFor.INDETERMINE, MotifFor.DEPART_HC, list.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10772397&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "[SIFISC-2939] Cas du contribuable n°10772397 (vérifie que l'algo ne crashe pas dans le cas où un couple hors-canton avec immeuble se marie, se divorce et puis se remarie mais cette fois en étant domicilié dans le canton; le tout dans le même année)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineCoupleHCAvecImmeubleMariageDivorcePuisReconciliationEtArriveeDansLeCantonLeToutLaMemeAnnee() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageSansFor(date(2000, 1, 1), date(2010, 6, 14), date(2010, 7, 8), date(2011, 6, 6));
		final MenageCommun ctb = ensemble.getMenage();

		addForPrincipal(ctb, date(2010, 5, 12), MotifFor.INDETERMINE, date(2010, 6, 14), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Geneve);
		addForSecondaire(ctb, date(2010, 5, 12), MotifFor.ACHAT_IMMOBILIER, date(2010, 6, 14), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Morges.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);


		addForPrincipal(ctb, date(2010, 7, 8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2011, 6, 6), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Morges);
		addForSecondaire(ctb, date(2010, 7, 8), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2011, 6, 6), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(2010, 1, 1), date(2010, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=30928601&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "[SIFISC-2939] Cas du contribuable n°30928601 (vérifie que l'algo ne crashe pas dans le cas où un contribuable possède deux fors fiscaux hors-Suisse disjoints dans la même année)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineContribuableAvecDeuxForFiscauxPrincipauxHSDisjointsDansLaMemeAnnee() throws Exception {

		final Contribuable ctb = createContribuableSansFor(30928601L);
		addForPrincipal(ctb, date(1976, 1, 7), MotifFor.INDETERMINE, date(1980, 1, 6), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		addForPrincipal(ctb, date(1980, 12, 30), MotifFor.INDETERMINE, MockPays.PaysInconnu);
		addForSecondaire(ctb, date(1980, 12, 30), MotifFor.ACHAT_IMMOBILIER, MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertHorsSuisse(date(1980, 12, 30), null, MotifFor.ACHAT_IMMOBILIER, null, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un contribuable PP vaudois avec immeuble qui se marie n'est pas assujetti dans l'année de son mariage")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMariagePPVaudoisAvecImmeuble() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10003678L);
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Morges);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Morges.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(2003, 1, 1), date(2005, 12, 31), MotifFor.INDETERMINE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage vaudois avec immeuble qui se sépare n'est pas assujetti dans l'année de la séparation")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSeparationMenageVaudoisAvecImmeuble() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 5, 23), date(2006, 9, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Morges);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Morges.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(2003, 1, 1), date(2005, 12, 31), MotifFor.INDETERMINE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un contribuable PP hors-canton avec immeuble qui se marie n'est pas assujetti dans l'année de son mariage (parce que le motif de fermeture du for secondaire est mariage)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMariagePPHorsCantonAvecImmeuble() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10003678L);
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Morges.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertHorsCanton(date(2003, 1, 1), date(2005, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage hors-canton avec immeuble qui se sépare n'est pas assujetti dans l'année de sa séparation (parce que le motif de fermeture du for secondaire est séparation)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSeparationMenageHorsCantonAvecImmeuble() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 5, 23), date(2006, 9, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Morges.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertHorsCanton(date(2003, 1, 1), date(2005, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un contribuable PP vaudois qui vend son immeuble et se marie dans l'année n'est pas assujetti dans l'année de son mariage (le couple recevra un déclaration et devra déclarer son immeuble dessus)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleEtMariageMemeAnneePPVaudois() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10003678L);
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(2003, 1, 1), date(2005, 12, 31), MotifFor.INDETERMINE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage vaudois qui vend son immeuble et se sépare dans l'année n'est pas assujetti dans l'année de sa séparation (chacun des composants recevra un déclaration et devronz déclarer l'immeuble dessus)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleEtSeparationMemeAnneeMenageVaudois() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 5, 23), date(2006, 9, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(2003, 1, 1), date(2005, 12, 31), MotifFor.INDETERMINE, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable n°10003678 (vérifie que l'assujettissement va jusqu'au 31 décembre de l'année courante lors de la vente d'un immeuble d'un contribuable hors-canton alors que le for principal se ferme avec motif mariage)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleEtMariageMemeAnneePPHorsCanton() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10003678L);
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges.getNoOFS(),MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertHorsCanton(date(2003, 1, 1), date(2006, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10003678&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage hors-canton qui vend son immeuble puis se sépare la même année est bien assujetti sur toute l'année de la séparation.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleEtSeparationMemeAnneeMenageHorsCanton() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 5, 23), date(2006, 9, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 5, 23), MotifFor.INDETERMINE, date(2006, 9, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 5, 23), MotifFor.ACHAT_IMMOBILIER, date(2006, 9, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges.getNoOFS(),MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertHorsCanton(date(2003, 1, 1), date(2006, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un contribuable PP qui arrive de hors-canton avec un immeuble puis se marie n'est pas assujetti sur l'année de son mariage (car l'assujettissment est supposé être reporté sur le couple)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHCAvecImmeublePuisMariageMemeAnneePP() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10010236L);
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 8, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage qui arrive de hors-canton avec un immeuble puis se sépare n'est pas assujetti sur l'année de la séparation (car les deux composants du couple seront assujettis individuellement et devront déclarer l'immeuble chacun de leur côté)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHCAvecImmeublePuisSeparationMemeAnneeMenage() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 7, 11), date(2003, 8, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable PP n°10000032 (vérifie que l'assujettissement hors-canton pour l'année 2003 est ignoré car il est supposé être reporté sur le couple)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHCVenteImmeublePuisMariageMemeAnneePP() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10010236L);
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 8, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage qui arrive de hors-canton avec un immeuble, le vend puis se sépare n'est pas assujetti sur l'année de la séparation (car les deux composants du couple seront assujettis individuellement et devront déclarer l'immeuble chacun de leur côté)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHCVenteImmeublePuisSeparationMemeAnneeMenage() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 7, 11), date(2003, 8, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 8, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie que l'assujettissement de la PP pour l'année 2003 est ignoré car il est supposé être reporté sur le couple")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleArriveeHCPuisMariageMemeAnneePP() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10010236L);
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 7, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifique qu'un ménage hors-canton qui vend son immeuble, arrive de hors-canton puis se sépare n'est pas assujetti sur l'année de la séparation (car les deux composants du couple seront assujettis individuellement et devront déclarer l'immeuble chacun de leur côté)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleArriveeHCPuisSeparationMemeAnneeMenage() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 7, 11), date(2003, 8, 1)).getMenage();
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 7, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie que le contribuable PP est bien assujetti sur toute l'année en raison de sa présence sur sol vaudois le 31 décembre 2003")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleArriveeHCPuisMariageEtEnfinSeparationMemeAnneePP() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10010236L);
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 7, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne);
		addForPrincipal(ctb, date(2003, 10, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne);

		final List<Assujettissement> list = service.determine(ctb);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(2003, 1, 1), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, list.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage commun hors-canton qui vend son immeuble, arrive de hors-canton, se sépare et se réconcile est bien assujetti sur toute l'année en raison de sa présence sur sol vaudois le 31 décembre 2003")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleArriveeHCPuisSeparationEtEnfinReconciliationMemeAnneeMenage() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 7, 11), date(2003, 8, 1), date(2003, 10, 23), null).getMenage();
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 7, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne);
		addForPrincipal(ctb, date(2003, 10, 23), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne);

		final List<Assujettissement> list = service.determine(ctb);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(2003, 1, 1), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, list.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie que le contribuable PP est bien assujetti sur toute l'année en tant que hors-canton immeuble en raison de la vente de son immeuble en début d'année (l'effet du mariage est annulé par la séparation dans la même année)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleArriveeHCPuisMariagePuisDepartHCEtEnfinSeparationMemeAnneePP() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10010236L);
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.RomanelSurLausanne);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 7, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(2003, 10, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Geneve);

		final List<Assujettissement> list = service.determine(ctb);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertHorsCanton(date(2003, 1, 1), date(2003, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.VENTE_IMMOBILIER, list.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10000032&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie qu'un ménage commun hors-canton qui vend son immeuble, arrive de hors-canton, se sépare, part hors-canton et se réconcile est bien assujetti sur toute l'année comme hors-canton.")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleArriveeHCPuisSeparationPuisDepartHCEtEnfinReconciliationMemeAnneeMenage() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(2003, 7, 11), date(2003, 8, 1), date(2003, 10, 23), null).getMenage();
		addForPrincipal(ctb, date(2003, 7, 11), MotifFor.INDETERMINE, date(2003, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		addForSecondaire(ctb, date(2003, 7, 11), MotifFor.ACHAT_IMMOBILIER, date(2003, 7, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.RomanelSurLausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(2003, 8, 1), MotifFor.ARRIVEE_HC, date(2003, 8, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.RomanelSurLausanne);
		addForPrincipal(ctb, date(2003, 10, 23), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Geneve);

		final List<Assujettissement> list = service.determine(ctb);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertHorsCanton(date(2003, 1, 1), date(2003, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.VENTE_IMMOBILIER, list.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=30928601&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Contribuable hors-canton qui achète et vend deux immeubles à deux moment instants d'une même année (vérifie que l'algo ne crashe pas)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineContribuableAvecDeuxImmeublesHCDisjointsDansLaMemeAnnee() throws Exception {

		final Contribuable ctb = createContribuableSansFor(30928601L);
		addForPrincipal(ctb, date(2007, 3, 7), MotifFor.INDETERMINE, date(2007, 5, 30), MotifFor.INDETERMINE, MockCommune.Neuchatel);
		addForPrincipal(ctb, date(2007, 9, 12), MotifFor.INDETERMINE, MockCommune.Geneve);
		addForSecondaire(ctb, date(2007, 3, 7), MotifFor.ACHAT_IMMOBILIER, date(2007, 5, 30), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2007, 9, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 11, 20), MotifFor.VENTE_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10010236&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable n°10010236 (vérifie que l'assujettissement hors-canton pour l'année 2004 est bien présent)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierMixteHCAchatImmeubleEtArriveeHCMemeAnnee() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10010236L);
		addForPrincipal(ctb, date(2004, 1, 29), MotifFor.INDETERMINE, date(2004, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
		final ForFiscalPrincipal ffp = addForPrincipal(ctb, date(2004, 2, 1), MotifFor.ARRIVEE_HC, MockCommune.Nyon);
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
		addForSecondaire(ctb, date(2004, 1, 29), MotifFor.ACHAT_IMMOBILIER, MockCommune.Nyon.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(2, liste.size());
		assertHorsCanton(date(2004, 1, 1), date(2004, 1, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.DEMENAGEMENT_VD, liste.get(0));
		assertSourcierMixteArt137Al2(date(2004, 2, 1), MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, liste.get(1));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=36502102&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable n°36502102 (vérifie que le contribuable n'est pas assujetti)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineCtbMariageEtSeparationDeuxFoisDansAnneeEtMotifsBizarres() throws Exception {

		final Contribuable ctb = createContribuableSansFor(36502102L);
		addForPrincipal(ctb, date(2001, 5, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2001, 12, 26), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Chamblon);
		addForPrincipal(ctb, date(2001, 12, 27), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2001, 12, 27), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Chamblon);

		assertEmpty(service.determine(ctb));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=36216757&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable PP n°36216757 (vérifie que le contribuable est bien assujetti sur toute l'année, malgré le motif de séparation qui manque)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePPMariageEtSeparationDansAnneeMaisMotifIndetermine() throws Exception {

		final Contribuable ctb = createContribuableSansFor(36502102L);
		addForPrincipal(ctb, date(1997, 1, 1), MotifFor.ARRIVEE_HS, date(2000, 3, 24), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Croy);
		addForPrincipal(ctb, date(2000, 10, 3), MotifFor.INDETERMINE, date(2001, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);
		addForPrincipal(ctb, date(2001, 2, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(1997, 1, 1), null, MotifFor.ARRIVEE_HS, null, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=36216757&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Vérifie que le ménage est bien assujetti sur toute l'année, malgré le motif de réconciliation qui manque)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineMenageSeparationEtReconciliationDansAnneeMaisMotifIndetermine() throws Exception {

		final Contribuable ctb = createMenageSansFor(10003678L, date(1997, 1, 1), date(2000, 3, 24), date(2000, 10, 3), null).getMenage();
		addForPrincipal(ctb, date(1997, 1, 1), MotifFor.ARRIVEE_HS, date(2000, 3, 24), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Croy);
		addForPrincipal(ctb, date(2000, 10, 3), MotifFor.INDETERMINE, date(2001, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);
		addForPrincipal(ctb, date(2001, 2, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(1, liste.size());
		assertOrdinaire(date(1997, 1, 1), null, MotifFor.ARRIVEE_HS, null, liste.get(0));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=10048078&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable PP n°10048078 (vérifie que l'algorithme ne crash pas en cas d'un départ hors-Suisse au 31 décembre)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseLe31Decembre() throws Exception {

		final Contribuable ctb = createContribuableSansFor(10048078L);
		addForPrincipal(ctb, date(2005, 10, 27), MotifFor.INDETERMINE, date(2007, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
		addForPrincipal(ctb, date(2007, 2, 1), MotifFor.ARRIVEE_HC, date(2007, 12, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(ctb, date(2007, 12, 31), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		addForSecondaire(ctb, date(2005, 10, 27), MotifFor.DEBUT_EXPLOITATION, date(2011, 3, 31), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(3, liste.size());
		assertHorsCanton(date(2005, 1, 1), date(2006, 12, 31), MotifFor.DEBUT_EXPLOITATION, MotifFor.ARRIVEE_HC, liste.get(0));
		assertOrdinaire(date(2007, 1, 1), date(2007, 12, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HS, liste.get(1));
		assertHorsSuisse(date(2007, 12, 31), date(2011, 3, 31), MotifFor.DEMENAGEMENT_VD, MotifFor.FIN_EXPLOITATION, liste.get(2));
	}

	@WebScreenshot(urls = "/fiscalite/unireg/web/tiers/timeline.do?id=41010811&print=true&title=${methodName}&description=${docDescription}")
	@WebScreenshotDoc(
			description = "Cas du contribuable PP n°41010811 (vérifie que l'algorithme calcul bien un assujettissement hors-Suisse de 1 jour pour le 27 décembre 1997)")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseEtArriveeHorsSuisseMemeJour() throws Exception {

		final Contribuable ctb = createContribuableSansFor(41010811L);
		addForPrincipal(ctb, date(1991, 1, 6), MotifFor.ARRIVEE_HS, date(1997, 12, 26), MotifFor.DEPART_HS, MockCommune.YverdonLesBains);
		addForPrincipal(ctb, date(1997, 12, 27), MotifFor.DEMENAGEMENT_VD, date(1997, 12, 27), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		addForSecondaire(ctb, date(1997, 12, 27), MotifFor.ACHAT_IMMOBILIER, date(1997, 12, 27), MotifFor.VENTE_IMMOBILIER, MockCommune.YverdonLesBains.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForPrincipal(ctb, date(1997, 12, 28), MotifFor.ARRIVEE_HS, MockCommune.YverdonLesBains);

		final List<Assujettissement> liste = service.determine(ctb);
		assertEquals(3, liste.size());
		assertOrdinaire(date(1991, 1, 6), date(1997, 12, 26), MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, liste.get(0));
		assertHorsSuisse(date(1997, 12, 27), date(1997, 12, 27), MotifFor.DEMENAGEMENT_VD, MotifFor.DEMENAGEMENT_VD, liste.get(1));
		assertOrdinaire(date(1997, 12, 28), null, MotifFor.ARRIVEE_HS, null, liste.get(2));
	}

	private static Set<Integer> buildSetFromArray(Integer... ints) {
		return new HashSet<Integer>(Arrays.asList(ints));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePourCommuneNonAssujetti() throws Exception {
		final Contribuable ctb = createContribuableSansFor();
		final List<Assujettissement> listeLausanneSansFor = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Lausanne.getNoOFSEtendu()));
		assertNull(listeLausanneSansFor);

		addForPrincipal(ctb, date(2000, 9, 4), MotifFor.ARRIVEE_HS, MockCommune.Renens);
		final List<Assujettissement> listeLausanneAvecForARenens = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Lausanne.getNoOFSEtendu()));
		assertNull(listeLausanneAvecForARenens);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePourCommuneHorsCantonImmeuble() throws Exception {
		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2000, 9, 4), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
		addForSecondaire(ctb, date(2000, 9, 4), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2005, 6, 24), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2002, 7, 12), MotifFor.ACHAT_IMMOBILIER, date(2006, 12, 2), MotifFor.VENTE_IMMOBILIER, MockCommune.Renens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> listeLausanne = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Lausanne.getNoOFSEtendu()));
		assertEquals(1, listeLausanne.size());
		assertHorsCanton(date(2000, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, listeLausanne.get(0));

		final List<Assujettissement> listeCroy = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Croy.getNoOFSEtendu()));
		assertEquals(1, listeCroy.size());
		assertHorsCanton(date(2005, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, listeCroy.get(0));

		final List<Assujettissement> listeRenens = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Renens.getNoOFSEtendu()));
		assertEquals(1, listeRenens.size());
		assertHorsCanton(date(2002, 1, 1), date(2006, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, listeRenens.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePourCommuneHorsSuisseImmeuble() throws Exception {

		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2000, 9, 4), MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
		addForSecondaire(ctb, date(2000, 9, 4), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2005, 6, 24), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2002, 7, 12), MotifFor.ACHAT_IMMOBILIER, date(2006, 12, 2), MotifFor.VENTE_IMMOBILIER, MockCommune.Renens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> listeLausanne = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Lausanne.getNoOFSEtendu()));
		assertEquals(1, listeLausanne.size());
		assertHorsSuisse(date(2000, 9, 4), null, MotifFor.ACHAT_IMMOBILIER, null, listeLausanne.get(0));

		final List<Assujettissement> listeCroy = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Croy.getNoOFSEtendu()));
		assertEquals(1, listeCroy.size());
		assertHorsSuisse(date(2005, 6, 24), null, MotifFor.ACHAT_IMMOBILIER, null, listeCroy.get(0));

		final List<Assujettissement> listeRenens = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Renens.getNoOFSEtendu()));
		assertEquals(1, listeRenens.size());
		assertHorsSuisse(date(2002, 7, 12), date(2006, 12, 2), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, listeRenens.get(0));

		final List<Assujettissement> listeCroyRenens = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Croy.getNoOFSEtendu(), MockCommune.Renens.getNoOFSEtendu()));
		assertEquals(1, listeCroyRenens.size());
		assertHorsSuisse(date(2002, 7, 12), null, MotifFor.ACHAT_IMMOBILIER, null, listeCroyRenens.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePourCommuneVaudois() throws Exception {

		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2000, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
		addForSecondaire(ctb, date(2000, 9, 4), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2005, 6, 24), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(ctb, date(2002, 7, 12), MotifFor.ACHAT_IMMOBILIER, date(2006, 12, 2), MotifFor.VENTE_IMMOBILIER, MockCommune.Renens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> listeAubonne = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Aubonne.getNoOFSEtendu()));
		assertEquals(1, listeAubonne.size());
		assertOrdinaire(date(2000, 6, 1), null, MotifFor.ARRIVEE_HS, null, listeAubonne.get(0));

		final List<Assujettissement> listeLausanne = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Lausanne.getNoOFSEtendu()));
		assertEquals(1, listeLausanne.size());
		assertHorsCanton(date(2000, 6, 1), null, MotifFor.ACHAT_IMMOBILIER, null, listeLausanne.get(0));

		final List<Assujettissement> listeCroy = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Croy.getNoOFSEtendu()));
		assertEquals(1, listeCroy.size());
		assertHorsCanton(date(2005, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, listeCroy.get(0));

		final List<Assujettissement> listeRenens = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Renens.getNoOFSEtendu()));
		assertEquals(1, listeRenens.size());
		assertHorsCanton(date(2002, 1, 1), date(2006, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, listeRenens.get(0));

		final List<Assujettissement> listeCroyRenens = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Croy.getNoOFSEtendu(), MockCommune.Renens.getNoOFSEtendu()));
		assertEquals(1, listeCroyRenens.size());
		assertHorsCanton(date(2002, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, listeCroyRenens.get(0));
	}

	/**
	 * [SIFISC-1769] Cas trouvé en testant le batch des rôles
	 * Exemple apparemment qui revient à chaque fois :
	 *  - for HS
	 *  - arrivée principale VD (mixte 2) avec for secondaire (et oui, mixte 2 quand-même !)dans autre commune (déjà présent ou débutant à l'arrivée, peu importe, apparemment)
	 *  - déménagement vers la commune du for secondaire dans la même PF que l'arrivée HS (apparemment même pas nécessaire dans 100.356.43)
	 *
	 *  Exemples de contribuables : 100.001.71, 100.247.12, 100.356.43...
	 *
	 *  -> le calcul de l'assujettissement sur la commune du for secondaire explose avec un chevauchement d'assujettissements trouvé
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePourCommuneSuivantArriveeHSAvecForSecondaireImmeubleDeMixte2() throws Exception {

		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2003, 4, 1), MotifFor.INDETERMINE, date(2005, 2, 18), MotifFor.DEPART_HS, MockCommune.Prilly, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);
		addForPrincipal(ctb, date(2005, 2, 19), MotifFor.DEMENAGEMENT_VD, date(2008, 5, 28), MotifFor.DEMENAGEMENT_VD, MockPays.France);
		// trou ici : pas de for principal entre le 29.5.2008 et le 6.7.2008
		addForPrincipal(ctb, date(2008, 7, 7), MotifFor.ARRIVEE_HS, date(2008, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);
		addForPrincipal(ctb, date(2008, 8, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);
		addForSecondaire(ctb, date(2008, 7, 7), MotifFor.ACHAT_IMMOBILIER, MockCommune.Vevey.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> listePrilly = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Prilly.getNoOFSEtendu()));
		assertEquals(1, listePrilly.size());
		assertSourcierMixteArt137Al2(date(2003, 1, 1), date(2005, 2, 18), MotifFor.INDETERMINE, MotifFor.DEPART_HS, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, listePrilly.get(0));

		final List<Assujettissement> listeBussigny = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Bussigny.getNoOFSEtendu()));
		assertNull(listeBussigny);

		// c'est ici que cela explosait !
		final List<Assujettissement> listeVevey = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Vevey.getNoOFSEtendu()));
		assertEquals(1, listeVevey.size());
		assertSourcierMixteArt137Al2(date(2008, 7, 7), null, MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, listeVevey.get(0));
	}

	/**
	 * [SIFISC-1769] Autre cas trouvé en testant le batch des rôles
	 *
	 * Vérifie que le calcul de l'assujettissement pour une commune ne provoque par de chevauchement en cas de passage de hors-canton source pur à vaudois mixte.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePourCommunePassageHCSourcePurAVaudoisMixte() throws Exception {

		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2007, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 7, 31), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
		final ForFiscalPrincipal ffp = addForPrincipal(ctb, date(2007, 8, 1), MotifFor.INDETERMINE, MockCommune.Aubonne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
		addForSecondaire(ctb, date(2007, 9, 6), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> liste = service.determinePourCommunes(ctb, buildSetFromArray(MockCommune.Aubonne.getNoOFSEtendu()));
		assertEquals(1, liste.size());
		assertSourcierMixteArt137Al2(date(2007, 1, 1), null, MotifFor.INDETERMINE, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, liste.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActiveForPrincipal() throws Exception {
		final Contribuable ctb = createUnForSimple();
		final List<Assujettissement> assujettissements = service.determine(ctb, RANGE_2000_2008, false);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActivePourHCImmeuble() throws Exception {
		final Contribuable ctb = createHorsCantonAvecImmeuble(date(2006, 3, 12));
		final List<Assujettissement> assujettissements = service.determine(ctb, RANGE_2006_2008, false);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Aubonne.getNoOFS()));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActivePourVaudoisImmeuble() throws Exception {
		final Contribuable ctb = createUnForSimple();
		addForSecondaire(ctb, date(2007, 4, 12), MotifFor.ACHAT_IMMOBILIER, date(2008, 6, 30), MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		for (int annee = 2006 ; annee < 2010 ; ++ annee) {
			final List<Assujettissement> assujettissements = service.determine(ctb, annee);
			final List<Integer> communesActives = new ArrayList<Integer>(2);
			communesActives.add(MockCommune.Lausanne.getNoOFS());
			if (annee >= 2007 && annee <= 2008) {
				communesActives.add(MockCommune.Cossonay.getNoOFS());
			}
			for (Assujettissement a : assujettissements) {
				assertCommunesActives(a, communesActives);
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActiveDemenagementVaudois() throws Exception {
		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2005, 2, 4), MotifFor.ARRIVEE_HS, date(2006, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(ctb, date(2006, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Leysin);

		for (int annee = 2005 ; annee <= 2007 ; ++ annee) {
			final List<Assujettissement> assujettissements = service.determine(ctb, annee);
			final List<Integer> communeActive = Arrays.asList(annee < 2006 ? MockCommune.Lausanne.getNoOFS() : MockCommune.Leysin.getNoOFS());
			for (Assujettissement a : assujettissements) {
				assertCommunesActives(a, communeActive);
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActiveDecesDansAnnee() throws Exception {
		final Contribuable ctb = createDecesVaudoisOrdinaire(date(1990, 4, 21), date(2005, 5, 12));
		final List<Assujettissement> assujettissements = service.determine(ctb, 2005);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActiveDepartHS() throws Exception {
		final Contribuable ctb = createDepartHorsSuisse(date(2005, 5, 12));
		final List<Assujettissement> assujettissements = service.determine(ctb, 2005);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneActiveDepartHSAvecImmeuble() throws Exception {
		final Contribuable ctb = createDepartHorsSuisseAvecImmeuble(date(2005, 5, 12));
		final List<Assujettissement> assujettissements = service.determine(ctb, 2005);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	private static void assertCommunesActives(Assujettissement assujettissement, List<Integer> noOfsCommunesActives) {
		final Set<Integer> actives;
		if (noOfsCommunesActives != null && !noOfsCommunesActives.isEmpty()) {
			actives = new HashSet<Integer>(noOfsCommunesActives);
		}
		else {
			actives = new HashSet<Integer>(0);
		}

		int nbActives = 0;
		for (int i = 0 ; i < 10000 ; ++ i) {
			final boolean expected = actives.contains(i);
			final boolean found = assujettissement.isActifSurCommune(i);
			assertEquals("Commune " + i, expected, found);
			if (found) {
				++ nbActives;
			}
		}

		if (noOfsCommunesActives == null) {
			assertEquals(0, nbActives);
		}
		else {
			assertEquals(noOfsCommunesActives.size(), nbActives);
		}
	}
}

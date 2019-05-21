package ch.vd.unireg.mouvement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;

@SuppressWarnings({"JavaDoc"})
public class DeterminerMouvementsDossiersEnMasseProcessorTest extends BusinessTest {

	private TiersDAO tiersDAO;
	private MouvementDossierDAO mouvementDossierDAO;
	private AssujettissementService assujettissementService;
	private AdresseService adresseService;

	private static final long noIndMarieParlotte = 1235125L;
	private static final RegDate dateNaissance = RegDate.get(1970, 3, 12);
	private static final RegDate dateMajorite = dateNaissance.addYears(18);

	private static final int noCaOidRolleAubonne = MockOfficeImpot.OID_ROLLE_AUBONNE.getNoColAdm();
	private static final int noCaOidLausanne = MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm();
	private static final int noCaOidVevey = MockOfficeImpot.OID_VEVEY.getNoColAdm();
	private static final int noCaOidOrbe = MockOfficeImpot.OID_ORBE.getNoColAdm();

	private long noOidRolleAubonne;
	private long noOidLausanne;
	private long noOidVevey;
	private long noOidOrbe;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		mouvementDossierDAO = getBean(MouvementDossierDAO.class, "mouvementDossierDAO");
		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		adresseService = getBean(AdresseService.class, "adresseService");

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndMarieParlotte, dateNaissance, "Marie-Parlotte", "Motordu", false);
			}
		});

		doInNewTransaction(status -> {
			noOidRolleAubonne = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ROLLE_AUBONNE.getNoColAdm()).getNumero();    // OID Rolle-Aubonne
			noOidLausanne = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()).getNumero();            // OID Lausanne
			noOidVevey = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_VEVEY.getNoColAdm()).getNumero();                  // OID Vevey
			noOidOrbe = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ORBE.getNoColAdm()).getNumero();                    // OID Orbe;
			return null;
		});
	}

	private DeterminerMouvementsDossiersEnMasseProcessor createProcessor() {
		return new DeterminerMouvementsDossiersEnMasseProcessor(tiersService, tiersDAO, mouvementDossierDAO, hibernateTemplate, transactionManager, assujettissementService, adresseService);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansFor() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// pas de for -> pas de mouvement
		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansChangementDeFor() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// pas de changement de for -> pas de mouvement
		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementSansChangementOID() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Croy);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vaulion);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// pas de changement d'OID -> pas de mouvement
		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVieuxDepartTousMouvements() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 2, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEPART_HS, MockCommune.Croy);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// vieux départ -> mouvement vers les archives de l'OID d'Orbe
		assertMouvementReceptionArchives(results, ctb, noOidOrbe);
		Assert.assertEquals(1, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidOrbe));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVieuxDepartArchivesSeulement() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = true;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 2, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEPART_HS, MockCommune.Croy);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// vieux départ -> mouvement vers les archives de l'OID d'Orbe
		assertMouvementReceptionArchives(results, ctb, noOidOrbe);
		Assert.assertEquals(1, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidOrbe));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// pas d'assujettissement n-2 -> pas de mouvement
		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDepartAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEPART_HS, MockCommune.Aubonne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// année juste après le départ -> pas encore de mouvement
		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementAnneeDerniereTousMouvements() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// mouvement d'envoi de Aubonne à Lausanne
		assertMouvementEnvoiEntreOid(results, ctb, noOidRolleAubonne, noOidLausanne);
		Assert.assertEquals(2, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidRolleAubonne));
		Assert.assertTrue(caCache.containsKey(noCaOidLausanne));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementAnneeDerniereSeulementArchives() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = true;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// normalement, mouvement d'envoi de Aubonne à Lausanne, mais comme on ne veut que les mouvements vers les archives,
		// le mouvement généré doit être un mouvement de réception vers les archives de Rolle/Aubonne
		assertMouvementReceptionArchives(results, ctb, noOidRolleAubonne);
		Assert.assertEquals(1, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidRolleAubonne));
	}

	private void assertMouvementReceptionArchives(DeterminerMouvementsDossiersEnMasseResults results, Contribuable ctb, long oid) {
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.mouvements.size());

		final DeterminerMouvementsDossiersEnMasseResults.Mouvement mvtResult = results.mouvements.get(0);
		Assert.assertNotNull(mvtResult);
		Assert.assertEquals((long) ctb.getNumero(), mvtResult.noCtb);
		Assert.assertTrue(mvtResult.getClass().getName(), mvtResult instanceof DeterminerMouvementsDossiersEnMasseResults.MouvementArchives);

		// dans la base ?
		final List<MouvementDossier> mvts = mouvementDossierDAO.findByNumeroDossier(ctb.getNumero(), false, true);
		Assert.assertNotNull(mvts);
		Assert.assertEquals(1, mvts.size());

		final MouvementDossier mvt = mvts.get(0);
		Assert.assertNotNull(mvt);
		Assert.assertTrue(mvt instanceof ReceptionDossierArchives);
		Assert.assertEquals(EtatMouvementDossier.A_TRAITER, mvt.getEtat());
		Assert.assertNull(mvt.getDateMouvement());

		final ReceptionDossierArchives receptionDossierArchives = (ReceptionDossierArchives) mvt;
		Assert.assertNotNull(receptionDossierArchives.getCollectiviteAdministrativeReceptrice());
		Assert.assertEquals(oid, (long) receptionDossierArchives.getCollectiviteAdministrativeReceptrice().getNumero());
	}

	private void assertMouvementEnvoiEntreOid(DeterminerMouvementsDossiersEnMasseResults results, Contribuable ctb, long oidSource, long oidDestinataire) {
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.mouvements.size());

		final DeterminerMouvementsDossiersEnMasseResults.Mouvement mvtResult = results.mouvements.get(0);
		Assert.assertNotNull(mvtResult);
		Assert.assertEquals((long) ctb.getNumero(), mvtResult.noCtb);
		Assert.assertTrue(mvtResult.getClass().getName(), mvtResult instanceof DeterminerMouvementsDossiersEnMasseResults.MouvementOid);

		// dans la base ?
		final List<MouvementDossier> mvts = mouvementDossierDAO.findByNumeroDossier(ctb.getNumero(), false, true);
		Assert.assertNotNull(mvts);
		Assert.assertEquals(1, mvts.size());

		final MouvementDossier mvt = mvts.get(0);
		Assert.assertNotNull(mvt);
		Assert.assertTrue(mvt instanceof EnvoiDossierVersCollectiviteAdministrative);
		Assert.assertEquals(EtatMouvementDossier.A_TRAITER, mvt.getEtat());
		Assert.assertNull(mvt.getDateMouvement());

		final EnvoiDossierVersCollectiviteAdministrative envoi = (EnvoiDossierVersCollectiviteAdministrative) mvt;
		Assert.assertNotNull(envoi.getCollectiviteAdministrativeDestinataire());
		Assert.assertNotNull(envoi.getCollectiviteAdministrativeEmettrice());
		Assert.assertEquals(oidDestinataire, (long) envoi.getCollectiviteAdministrativeDestinataire().getNumero());
		Assert.assertEquals(oidSource, (long) envoi.getCollectiviteAdministrativeEmettrice().getNumero());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testArriveeEtDemenagementAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateArrivee = RegDate.get(dateTraitement.year() - 1, 2, 12);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// mouvement d'envoi de Aubonne à Lausanne
		assertMouvementEnvoiEntreOid(results, ctb, noOidRolleAubonne, noOidLausanne);
		Assert.assertEquals(2, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidRolleAubonne));
		Assert.assertTrue(caCache.containsKey(noCaOidLausanne));
	}

	/**
	 * C'est le cas décrit dans le cas JIRA UNIREG-2434
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementIlYADeuxAnsPuisEncoreAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate datePremierDemenagement = RegDate.get(dateTraitement.year() - 2, 6, 30);
		final RegDate dateDeuxiemeDemenagement = RegDate.get(dateTraitement.year() - 1, 3, 1);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, datePremierDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, datePremierDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, dateDeuxiemeDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(ctb, dateDeuxiemeDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);

		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// mouvement d'envoi de Lausanne à Vevey
		assertMouvementEnvoiEntreOid(results, ctb, noOidLausanne, noOidVevey);
		Assert.assertEquals(2, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidVevey));
		Assert.assertTrue(caCache.containsKey(noCaOidLausanne));
	}

	/**
	 * C'est le cas décrit dans le cas JIRA UNIREG-2555
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSeparationPuisDemenagementAnneeDerniere() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateSeparation = RegDate.get(dateTraitement.year() - 1, 6, 25);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 7, 1);

		addForPrincipal(ctb, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// mouvement d'envoi de Rollo-Aubonne à Lausanne
		assertMouvementEnvoiEntreOid(results, ctb, noOidRolleAubonne, noOidLausanne);
		Assert.assertEquals(2, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidRolleAubonne));
		Assert.assertTrue(caCache.containsKey(noCaOidLausanne));
	}

	/**
	 * C'est le cas décrit dans le cas jira UNIREG-2854
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonAchatImmeubleAnneeDerniere() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDepartHC = date(dateTraitement.year() - 7, 8, 30);
		final RegDate dateAchatImmeuble = date(dateTraitement.year() - 1, 5, 12);

		addForPrincipal(ctb, dateDepartHC.addYears(-1), MotifFor.ARRIVEE_HS, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(ctb, dateDepartHC.addDays(1), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(ctb, dateAchatImmeuble, MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}

	private void assertPasDeMouvement(DeterminerMouvementsDossiersEnMasseResults results, Contribuable ctb) {

		// dans le rapport
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(0, results.mouvements.size());

		// pas de mouvement en base non plus ?
		final List<MouvementDossier> mvts = mouvementDossierDAO.findByNumeroDossier(ctb.getNumero(), false, true);
		if (mvts != null) {
			Assert.assertEquals(0, mvts.size());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierPur() throws Exception {

		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.SOURCE);

		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		// sourcier pur -> devrait être indiqué comme ignoré (et donc pas de mouvement)
		assertPasDeMouvement(results, ctb);
		Assert.assertNotNull(results.ignores);
		Assert.assertEquals(1, results.ignores.size());

		final DeterminerMouvementsDossiersEnMasseResults.NonTraite ignore = results.ignores.get(0);
		Assert.assertEquals(DeterminerMouvementsDossiersEnMasseResults.Raison.SOURCIER_PUR, ignore.type);
		Assert.assertEquals(ctb.getNumero(), (Long) ignore.noCtb);
	}

	@Test
	public void testMixteDirectementOuvertHS() throws Exception {

		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;

		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		final long numero = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1950, 5, 21), Sexe.MASCULIN);
			pp.setNumeroOfsNationalite(MockPays.RoyaumeUni.getNoOFS());

			addForPrincipal(pp, date(dateTraitement.year() - 1, 4, 12), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Albanie, ModeImposition.MIXTE_137_1);
			return pp.getNumero();
		});

		final DeterminerMouvementsDossiersEnMasseResults results = proc.run(dateTraitement, archivesSeulement, null);

		Assert.assertNotNull(results.erreurs);
		Assert.assertEquals(0, results.erreurs.size()); // [SIFISC-8095] Un for fiscal mixte 137 hors-Suisse est interprété comme un for source pur, donc pas de dossier et pas d'erreur
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPassageDeSourceAuRoleAnneeDerniere() throws Exception {

		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final boolean archivesSeulement = false;
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement, tiersService, adresseService);

		final PersonnePhysique ctb = addHabitant(noIndMarieParlotte);
		final RegDate datePermisC = date(dateTraitement.year() - 1, 6, 12);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, datePermisC.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, ModeImposition.SOURCE);
		addForPrincipal(ctb, datePermisC, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);

		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<>();
		proc.traiterContribuable(ctb, ranges, archivesSeulement, caCache, results);

		assertPasDeMouvement(results, ctb);
		Assert.assertEquals(0, caCache.size());
	}
}

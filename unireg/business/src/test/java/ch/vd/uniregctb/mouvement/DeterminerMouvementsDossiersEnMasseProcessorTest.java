package ch.vd.uniregctb.mouvement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.MotifFor;

public class DeterminerMouvementsDossiersEnMasseProcessorTest extends BusinessTest {

	private TiersDAO tiersDAO;
	private MouvementDossierDAO mouvementDossierDAO;

	private static final long noIndMarieParlotte = 1235125L;
	private static final RegDate dateNaissance = RegDate.get(1970, 3, 12);
	private static final RegDate dateMajorite = dateNaissance.addYears(18);

	private static final int noCaOidRolleAubonne = 2;
	private static final int noCaOidLausanne = 7;
	private static final int noCaOidVevey = 18;

	private long noOidRolleAubonne;
	private long noOidLausanne;
	private long noOidVevey;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		mouvementDossierDAO = getBean(MouvementDossierDAO.class, "mouvementDossierDAO");

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndMarieParlotte, dateNaissance, "Marie-Parlotte", "Motordu", false);
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				noOidRolleAubonne = tiersService.getOrCreateCollectiviteAdministrative(noCaOidRolleAubonne).getNumero();    // OID Rolle-Aubonne
				noOidLausanne = tiersService.getOrCreateCollectiviteAdministrative(noCaOidLausanne).getNumero();            // OID Lausanne
				noOidVevey = tiersService.getOrCreateCollectiviteAdministrative(noCaOidVevey).getNumero();                  // OID Vevey
				return null;
			}
		});
	}

	private DeterminerMouvementsDossiersEnMasseProcessor createProcessor() {
		return new DeterminerMouvementsDossiersEnMasseProcessor(tiersService, tiersDAO, mouvementDossierDAO, hibernateTemplate, transactionManager);
	}

	@Test
	public void testSansFor() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// pas de for -> pas de mouvement
		assertPasDeMouvement(ctb, results);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	public void testSansChangementDeFor() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// pas de changement de for -> pas de mouvement
		assertPasDeMouvement(ctb, results);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	public void testDemenagementSansChangementOID() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Croy);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vaulion);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// pas de changement d'OID -> pas de mouvement
		assertPasDeMouvement(ctb, results);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	public void testArriveeAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// pas d'assujettissement n-2 -> pas de mouvement
		assertPasDeMouvement(ctb, results);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	public void testDepartAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEPART_HS, MockCommune.Aubonne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// année juste après le départ -> pas encore de mouvement
		assertPasDeMouvement(ctb, results);
		Assert.assertEquals(0, caCache.size());
	}

	@Test
	public void testPartiIlYADeuxAns() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 2, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEPART_HS, MockCommune.Aubonne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// mouvement vers les archives attendu
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.mouvements.size());
		Assert.assertEquals(1, caCache.size());

		final DeterminerMouvementsDossiersEnMasseResults.Mouvement mvtResult = results.mouvements.get(0);
		Assert.assertNotNull(mvtResult);
		Assert.assertEquals((long) ctb.getNumero(), mvtResult.noCtb);
		Assert.assertTrue(mvtResult.getClass().getName(), mvtResult instanceof DeterminerMouvementsDossiersEnMasseResults.MouvementArchives);
		Assert.assertEquals(noCaOidRolleAubonne, (int) caCache.keySet().iterator().next());

		// dans la base ?
		final Set<MouvementDossier> mvts = ctb.getMouvementsDossier();
		Assert.assertNotNull(mvts);
		Assert.assertEquals(1, mvts.size());

		final MouvementDossier mvt = mvts.iterator().next();
		Assert.assertNotNull(mvt);
		Assert.assertTrue(mvt instanceof ReceptionDossierArchives);
		Assert.assertEquals(EtatMouvementDossier.A_TRAITER, mvt.getEtat());
		Assert.assertNull(mvt.getDateMouvement());

		final ReceptionDossierArchives reception = (ReceptionDossierArchives) mvt;
		Assert.assertNotNull(reception.getCollectiviteAdministrativeReceptrice());
		Assert.assertEquals(noOidRolleAubonne, (long) reception.getCollectiviteAdministrativeReceptrice().getNumero());
	}

	@Test
	public void testDemenagementAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// mouvement d'envoi de Aubonne à Lausanne
		assertMouvementEnvoiEntreOid(results, ctb, noOidRolleAubonne, noOidLausanne);
		Assert.assertEquals(2, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidRolleAubonne));
		Assert.assertTrue(caCache.containsKey(noCaOidLausanne));
	}

	private void assertMouvementEnvoiEntreOid(DeterminerMouvementsDossiersEnMasseResults results, Contribuable ctb, long oidSource, long oidDestinataire) {
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(1, results.mouvements.size());

		final DeterminerMouvementsDossiersEnMasseResults.Mouvement mvtResult = results.mouvements.get(0);
		Assert.assertNotNull(mvtResult);
		Assert.assertEquals((long) ctb.getNumero(), mvtResult.noCtb);
		Assert.assertTrue(mvtResult.getClass().getName(), mvtResult instanceof DeterminerMouvementsDossiersEnMasseResults.MouvementOid);

		// dans la base ?
		final Set<MouvementDossier> mvts = ctb.getMouvementsDossier();
		Assert.assertNotNull(mvts);
		Assert.assertEquals(1, mvts.size());

		final MouvementDossier mvt = mvts.iterator().next();
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
	public void testArriveeEtDemenagementAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateArrivee = RegDate.get(dateTraitement.year() - 1, 2, 12);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 6, 30);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

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
	public void testDemenagementIlYADeuxAnsPuisEncoreAnneeDerniere() throws Exception {
		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate datePremierDemenagement = RegDate.get(dateTraitement.year() - 2, 6, 30);
		final RegDate dateDeuxiemeDemenagement = RegDate.get(dateTraitement.year() - 1, 3, 1);
		addForPrincipal(ctb, dateMajorite, MotifFor.MAJORITE, datePremierDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, datePremierDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, dateDeuxiemeDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(ctb, dateDeuxiemeDemenagement.addDays(1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);

		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

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
	public void testSeparationPuisDemenagementAnneeDerniere() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles ranges = new DeterminerMouvementsDossiersEnMasseProcessor.RangesUtiles(dateTraitement);
		final DeterminerMouvementsDossiersEnMasseResults results = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		final Contribuable ctb = addHabitant(noIndMarieParlotte);
		final RegDate dateSeparation = RegDate.get(dateTraitement.year() - 1, 6, 25);
		final RegDate dateDemenagement = RegDate.get(dateTraitement.year() - 1, 7, 1);

		addForPrincipal(ctb, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
		addForPrincipal(ctb, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);

		final DeterminerMouvementsDossiersEnMasseProcessor proc = createProcessor();
		final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>();
		proc.traiterContribuable(ctb, ranges, caCache, results);

		// mouvement d'envoi de Rollo-Aubonne à Lausanne
		assertMouvementEnvoiEntreOid(results, ctb, noOidRolleAubonne, noOidLausanne);
		Assert.assertEquals(2, caCache.size());
		Assert.assertTrue(caCache.containsKey(noCaOidRolleAubonne));
		Assert.assertTrue(caCache.containsKey(noCaOidLausanne));

	}

	private void assertPasDeMouvement(Contribuable ctb, DeterminerMouvementsDossiersEnMasseResults results) {

		// dans le rapport
		Assert.assertEquals(1, results.getNbContribuablesInspectes());
		Assert.assertEquals(0, results.mouvements.size());

		// pas de mouvement en base non plus ?
		final Set<MouvementDossier> mvts = ctb.getMouvementsDossier();
		if (mvts != null) {
			Assert.assertEquals(0, mvts.size());
		}
	}
}

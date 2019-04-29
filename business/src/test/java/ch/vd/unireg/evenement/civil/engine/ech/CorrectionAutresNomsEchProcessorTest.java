package ch.vd.unireg.evenement.civil.engine.ech;

import net.sf.ehcache.CacheManager;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.cache.ServiceCivilCache;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CorrectionAutresNomsEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCorrectionAutresNoms() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}

	@Test(timeout = 10000L)
	public void testCorrectionCorrectionAutresNoms() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.CORRECTION);
	}

	@Test(timeout = 10000L)
	public void testAnnulationCorrectionAutresNoms() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.ANNULATION);
	}

	/**
	 * [SIFISC-4900] Ce test vérifie que: <ul> <li>l'arrivée d'un événement de correction de noms déclenche bien l'indexation du tiers concerné</li> <li>le cache du service civil est bien mis-à-jour</li>
	 * </ul>
	 */
	@Test(timeout = 30000L)
	public void testIndexationPourCorrectionAutresNoms() throws Exception {

		setWantIndexationTiers(true);

		final long noIndividu = 126673246L;
		final RegDate dateEvt = date(2011, 10, 31);
		final RegDate dateNaissance = date(1956, 4, 23);

		// créée le service civil et un cache par devant
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(getBean(CacheManager.class, "ehCacheManager"));
		cache.setCacheName("serviceCivil");
		cache.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		cache.setTarget(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				MockIndividu gerard = addIndividu(noIndividu, dateNaissance, "Manfind", "Gérard", true);
				addAdresse(gerard, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateNaissance, null);
			}
		});
		cache.afterPropertiesSet();
		try {
			serviceCivil.setUp(cache);

			// mise en place fiscale
			final long noPP = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// Etat de l'indexation avant traitement
			final TiersIndexedData indexed = globalTiersSearcher.get(noPP);
			assertNotNull(indexed);
			assertEquals("Gérard Manfind", indexed.getNom1());

			doModificationIndividu(noIndividu, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setNom("Findman");
				}
			});

			// événement de Correction d'adresse
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(11824L);
				evt.setType(TypeEvenementCivilEch.CORR_AUTRES_NOMS);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateEvt);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
			});

			// les données du cache du service civil doivent être correctes
			final Individu individuAfter = serviceCivil.getIndividu(noIndividu, null);
			assertNotNull(individuAfter);
			assertEquals("Findman", individuAfter.getNom());

			globalTiersIndexer.sync();

			// Etat de l'indexation avant traitement
			final TiersIndexedData indexedAfter = globalTiersSearcher.get(noPP);
			assertNotNull(indexedAfter);
			assertEquals("Gérard Findman", indexedAfter.getNom1());
		}
		finally {
			cache.destroy();
		}
	}
}

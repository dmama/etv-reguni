package ch.vd.uniregctb.evenement.engine;

import java.util.List;

import net.sf.ehcache.CacheManager;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.jms.EvenementCivilListener;
import ch.vd.uniregctb.evenement.jms.EvenementCivilUnitaireListenerTest;
import ch.vd.uniregctb.evenement.jms.MockEsbMessage;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilCache;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class EvenementCivilTest extends BusinessTest {

	private EvenementCivilDAO evenementCivilDAO;
	private EvenementCivilProcessor evenementCivilProcessor;
	private EvenementCivilListener evenementCivilListener;
	private GlobalTiersSearcher searcher;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementCivilDAO = getBean(EvenementCivilDAO.class, "evenementCivilDAO");
		evenementCivilProcessor = getBean(EvenementCivilProcessor.class, "evenementCivilProcessor");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

		evenementCivilListener = new EvenementCivilListener();
		evenementCivilListener.setEvenementCivilProcessor(evenementCivilProcessor);
		evenementCivilListener.setTransactionManager(transactionManager);
		evenementCivilListener.setEvenementCivilDAO(evenementCivilDAO);
	}

	/**
	 * Ce test vérifie que l'arrivée d'un événement civil provoque bien l'invalidation de l'individu correspondant dans le cache du service
	 * civil.
	 */
	@NotTransactional
	@Test
	public void testInvalidationDuCache() throws Exception {

		setWantIndexation(true);

		/*
		 * Préparation
		 */

		// Initialisation du service civil avec un cache
		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(cacheManager);
		cache.setCacheName("serviceCivil");
		serviceCivil.setUp(cache);

		final long jeanNoInd = 1234;

		// Création de l'individu
		cache.setTarget(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu jean = addIndividu(jeanNoInd, date(1975, 3, 2), "Jacquouille", "Jean", true);
				addAdresse(jean, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(1975, 3, 2), null);
			}
		});

		// Crée le contribuable correspondant
		final Long jeanId = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jean = addHabitant(jeanNoInd);
				addForPrincipal(jean, date(1993, 3, 2), MotifFor.MAJORITE, MockCommune.Lausanne);
				return jean.getNumero();
			}
		});

		globalTiersIndexer.sync();		

		// On vérifie que le tiers est bien présent dans le cache
		assertNomIndividu("Jacquouille", "Jean", cache, jeanNoInd);

		// On vérifie que le tiers est indexé correctement
		assertNomIndexer("Jacquouille", "Jean", jeanId);

		/*
		 * Réception d'un événement de changement de nom au 1er janvier 2009
		 */

		// Changement du nom dans le service civil (on réinitialise complétement le service pour simuler la présence de nouveaux objets
		// comme c'est le cas avec le service de host-interface)
		cache.setTarget(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu jean = addIndividu(jeanNoInd, date(1975, 3, 2), "Jacquouille", "Jean", true);
				HistoriqueIndividu h = new MockHistoriqueIndividu(RegDate.get(2009, 1, 1), "Jacquard", "Jean");
				jean.addHistoriqueIndividu(h);
				addAdresse(jean, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						date(1975, 3, 2), null);
			}
		});

		// Simulation de l'arrivée de l'événement civil
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final String body = EvenementCivilUnitaireListenerTest.createMessage(1, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM.getId(), jeanNoInd, RegDate.get(2009, 1, 1), MockCommune.Lausanne.getNoOFS());
				final EsbMessage message = new MockEsbMessage(body);
				evenementCivilListener.onEsbMessage(message);
				return null;
			}
		});

		// L'événement civil doit avoir été traité
		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<EvenementCivilData> evenements = evenementCivilDAO.getAll();
				assertNotNull(evenements);
				assertEquals(1, evenements.size());
				assertEquals(EtatEvenementCivil.TRAITE, evenements.get(0).getEtat());

				globalTiersIndexer.sync();

				// On vérifie que le tiers a bien été mis-à-jour dans le cache ...
				assertNomIndividu("Jacquard", "Jean", cache, jeanNoInd);
				// ... et que le tiers a bien été mis-à-jour dans l'indexeur
				assertNomIndexer("Jacquard", "Jean", jeanId);

				return null;
			}
		});
	}

	private void assertNomIndexer(String nom, String prenom, final Long tiersId) {
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison(nom + " " + prenom);
		final List<TiersIndexedData> res = searcher.search(criteria);
		assertNotNull(res);
		assertEquals(1, res.size());

		final TiersIndexedData res0 = res.get(0);
		assertNotNull(res0);
		assertEquals(tiersId, res0.getNumero());
		assertEquals(nom + " " + prenom, res0.getNom1());
	}

	private static void assertNomIndividu(String nom, String prenom, ServiceCivilCache cache, final long noIndividu) {
		final Individu individu = cache.getIndividu(noIndividu, 2400);
		assertNotNull(individu);
		assertEquals(prenom, individu.getDernierHistoriqueIndividu().getPrenom());
		assertEquals(nom, individu.getDernierHistoriqueIndividu().getNom());
	}
}

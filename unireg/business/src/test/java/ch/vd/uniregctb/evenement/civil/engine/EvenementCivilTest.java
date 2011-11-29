package ch.vd.uniregctb.evenement.civil.engine;

import java.util.List;

import net.sf.ehcache.CacheManager;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.evenement.civil.externe.jms.EvenementCivilListener;
import ch.vd.uniregctb.evenement.civil.externe.jms.EvenementCivilListenerTest;
import ch.vd.uniregctb.evenement.civil.externe.jms.MockEsbMessage;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilCache;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class EvenementCivilTest extends BusinessTest {

	private EvenementCivilExterneDAO evenementCivilExterneDAO;
	private EvenementCivilListener evenementCivilListener;
	private GlobalTiersSearcher searcher;
	private EvenementCivilAsyncProcessor evenementCivilProcessor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementCivilExterneDAO = getBean(EvenementCivilExterneDAO.class, "evenementCivilExterneDAO");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

		evenementCivilProcessor = getBean(EvenementCivilAsyncProcessor.class, "evenementCivilAsyncProcessor");
		evenementCivilListener = new EvenementCivilListener();
		evenementCivilListener.setEvenementCivilAsyncProcessor(evenementCivilProcessor);
		evenementCivilListener.setTransactionManager(transactionManager);
		evenementCivilListener.setEvenementCivilExterneDAO(evenementCivilExterneDAO);
	}

	/**
	 * Ce test vérifie que l'arrivée d'un événement civil provoque bien l'invalidation de l'individu correspondant dans le cache du service
	 * civil.
	 */
	@Test
	public void testInvalidationDuCache() throws Exception {

		setWantIndexation(true);

		/*
		 * Préparation
		 */

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final DataEventService dataEventService = getBean(DataEventService.class, "dataEventService");
		assertNotNull(dataEventService);

		final UniregCacheManager uniregCacheManager = getBean(UniregCacheManager.class, "uniregCacheManager");
		assertNotNull(uniregCacheManager);

		// Initialisation du service civil avec un cache
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(cacheManager);
		cache.setCacheName("serviceCivil");
		cache.setUniregCacheManager(uniregCacheManager);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
		cache.reset();
		try {
			serviceCivil.setUp(cache);

			final long jeanNoInd = 1234;

			// Création de l'individu
			cache.setTarget(new MockServiceCivil() {
				@Override
				protected void init() {
					MockIndividu jean = addIndividu(jeanNoInd, date(1975, 3, 2), "Jacquouille", "Jean", true);
					addAdresse(jean, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
							date(1975, 3, 2), null);
				}
			});

			// Crée le contribuable correspondant
			final Long jeanId = doInNewTransaction(new TxCallback<Long>() {
				@Override
				public Long execute(TransactionStatus status) throws Exception {
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
					addAdresse(jean, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
							date(1975, 3, 2), null);
				}
			});

			// Simulation de l'arrivée de l'événement civil
			doInNewTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					final String body = EvenementCivilListenerTest.createMessage(1, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM.getId(), jeanNoInd, RegDate.get(2009, 1, 1), MockCommune.Lausanne.getNoOFS());
					EvenementCivilListenerTest.sendMessageSync(evenementCivilListener, body);
					return null;
				}
			});

			// L'événement civil doit avoir été traité
			doInTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {

					final List<EvenementCivilExterne> evenements = evenementCivilExterneDAO.getAll();
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
		finally {
			serviceCivil.tearDown();
			cache.destroy();
		}
	}

	/**
	 * Ce test vérifie que le user de création de l'évenement civil a bien
	 * été initialisé avec le visa de mutation provenant de RcPers ou REgPP.
	 */
	@Test
	public void testVisaMutation() throws Exception {

		final long jeanNoInd = 1234;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(jeanNoInd, date(1978, 3, 2), "De la Fontaine", "Jean", true);
			}
		});

		// Crée le contribuable correspondant
		final Long jeanId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique jean = addHabitant(jeanNoInd);
				addForPrincipal(jean, date(1993, 3, 2), MotifFor.MAJORITE, MockCommune.Lausanne);
				return jean.getNumero();
			}
		});

		// Simulation de l'arrivée de l'événement civil
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final String body = EvenementCivilListenerTest.createMessage(1, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM.getId(), jeanNoInd, RegDate.get(2009, 1, 1), MockCommune.Lausanne.getNoOFS());
				final EsbMessage message = new MockEsbMessage(body);
				message.setBusinessUser("VISA_MUTATION");
				evenementCivilListener.onEsbMessage(message);

				// la persistence en base de l'événement civil est synchrone -> en théorie, ce n'est pas la peine d'attendre le traitement
				// mais en fait si, parce que sinon l'événement est traité alors qu'un autre test tourne et produit une erreur (la base a
				// potentiellement été rafraîchie plusieurs fois depuis)
				evenementCivilProcessor.sync();

				return null;
			}
		});

		// L'événement civil doit avoir été traité
		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementCivilExterne> evenements = evenementCivilExterneDAO.getAll();
				assertNotNull(evenements);
				assertEquals(1, evenements.size());
				// le visa de mutation doit être present
				assertEquals("VISA_MUTATION",evenements.get(0).getLogCreationUser());
				globalTiersIndexer.sync();
				return null;
			}
		});
	}

	private void assertNomIndexer(String nom, String prenom, final Long tiersId) {
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison(prenom + ' ' + nom);
		final List<TiersIndexedData> res = searcher.search(criteria);
		assertNotNull(res);
		assertEquals(1, res.size());

		final TiersIndexedData res0 = res.get(0);
		assertNotNull(res0);
		assertEquals(tiersId, res0.getNumero());
		assertEquals(prenom + ' ' + nom, res0.getNom1());
	}

	private static void assertNomIndividu(String nom, String prenom, ServiceCivilCache cache, final long noIndividu) {
		final Individu individu = cache.getIndividu(noIndividu, 2400);
		assertNotNull(individu);
		assertEquals(prenom, individu.getDernierHistoriqueIndividu().getPrenom());
		assertEquals(nom, individu.getDernierHistoriqueIndividu().getNom());
	}
}

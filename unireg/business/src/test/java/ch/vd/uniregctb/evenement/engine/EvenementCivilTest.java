package ch.vd.uniregctb.evenement.engine;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import net.sf.ehcache.CacheManager;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaireDAO;
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

@SuppressWarnings({"JavaDoc"})
public class EvenementCivilTest extends BusinessTest {

	private EvenementCivilUnitaireDAO evenementCivilUnitaireDAO;
	private EvenementCivilRegroupeDAO evenementCivilRegroupeDAO;
	private EvenementCivilRegrouper evenementCivilRegrouper;
	private EvenementCivilProcessor evenementCivilProcessor;
	private GlobalTiersSearcher searcher;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementCivilUnitaireDAO = getBean(EvenementCivilUnitaireDAO.class, "evenementCivilUnitaireDAO");
		evenementCivilRegroupeDAO = getBean(EvenementCivilRegroupeDAO.class, "evenementCivilRegroupeDAO");
		evenementCivilRegrouper = getBean(EvenementCivilRegrouper.class, "evenementCivilRegrouper");
		evenementCivilProcessor = getBean(EvenementCivilProcessor.class, "evenementCivilProcessor");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
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

		ServiceCivilCache cache = new ServiceCivilCache();
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
				addEvCivUnit(1, RegDate.get(2009, 1, 1), MockCommune.Lausanne, jeanNoInd, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				evenementCivilRegrouper.regroupeTousEvenementsNonTraites(null);
				return null;
			}
		});

		// L'événement civil doit avoir été traité
		final List<EvenementCivilUnitaire> evsCivils = evenementCivilUnitaireDAO.getAll();
		assertNotNull(evsCivils);
		assertEquals(1, evsCivils.size());
		assertEquals(EtatEvenementCivil.TRAITE, evsCivils.get(0).getEtat());

		// On vérifie que le tiers a bien été mis-à-jour dans le cache ...
		assertNomIndividu("Jacquard", "Jean", cache, jeanNoInd);
		// ... mais que l'indexeur est toujours sur l'ancien nom puisque l'événement regroupé n'a pas encore été traité
		assertNomIndexer("Jacquouille", "Jean", jeanId);

		/*
		 * Traitement de l'événement de changement de nom
		 */

		// Traitement des événements regroupés
		evenementCivilProcessor.traiteEvenementsCivilsRegroupes(null);

		// L'événement regroupé doit avoir été traité
		final List<EvenementCivilRegroupe> evsRegroupes = evenementCivilRegroupeDAO.getAll();
		assertNotNull(evsRegroupes);
		assertEquals(1, evsRegroupes.size());
		assertEquals(EtatEvenementCivil.TRAITE, evsRegroupes.get(0).getEtat());

		// On vérifie que le tiers a bien été mis-à-jour dans l'indexeur ...
		assertNomIndexer("Jacquard", "Jean", jeanId);
		// ... et que l'individu est toujours correct dans le cache
		assertNomIndividu("Jacquard", "Jean", cache, jeanNoInd);
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

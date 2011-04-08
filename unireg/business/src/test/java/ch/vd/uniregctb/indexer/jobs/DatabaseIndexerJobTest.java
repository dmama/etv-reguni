package ch.vd.uniregctb.indexer.jobs;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class DatabaseIndexerJobTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(DatabaseIndexerJobTest.class);

	private static final String DBUNIT_FILE = "DatabaseIndexerJobTest.xml";

	private TiersDAO tiersDAO;


	public DatabaseIndexerJobTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		servicePM.setUp(new DefaultMockServicePM());

		serviceCivil.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu alain = addIndividu(9876, RegDate.get(1976, 2, 27), "Dupont", "Alain", true);
				MockIndividu richard = addIndividu(9734, RegDate.get(1942, 12, 7), "Bolomey", "Richard", true);
				MockIndividu james = addIndividu(1373, RegDate.get(1992, 1, 14), "Dean", "James", true);
				MockIndividu francois = addIndividu(403399, RegDate.get(1961, 3, 12), "Lestourgie", "Francois", true);
				MockIndividu claudine = addIndividu(222, RegDate.get(1975, 11, 30), "Duchene", "Claudine", false);
				MockIndividu alain2 = addIndividu(111, RegDate.get(1965, 5, 21), "Dupont", "Alain", true);
				MockIndividu miro = addIndividu(333, RegDate.get(1972, 7, 15), "Boillat dupain", "Miro", true);
				MockIndividu claudine2 = addIndividu(444, RegDate.get(1922, 2, 12), "Duchene", "Claudine", false);

				addFieldsIndividu(richard, "1234567891023", "98765432109", null);

				addDefaultAdressesTo(alain);
				addDefaultAdressesTo(richard);
				addDefaultAdressesTo(james);
				addDefaultAdressesTo(francois);
				addDefaultAdressesTo(claudine);
				addDefaultAdressesTo(alain2);
				addDefaultAdressesTo(miro);
				addDefaultAdressesTo(claudine2);
			}

			private void addDefaultAdressesTo(MockIndividu individu) {
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 11, 2), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 11, 2), null);
			}
		});

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());

		loadDatabase(DBUNIT_FILE);
	}

	@Test
	public void testReindexationJob() throws Exception {

		globalTiersIndexer.setOnTheFlyIndexation(false);
		doInNewTransaction(new TxCallback() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Ajout d'un Habitant qui ne se reindexe pas
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(12345678L);
				hab.setNumeroIndividu(123456L);
				tiersDAO.save(hab);
				return null;
			}

		});
		globalTiersIndexer.setOnTheFlyIndexation(true);

		// Le tiers est chargé
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(7823L);
			List<?> l = globalTiersSearcher.search(criteria);
			assertEquals(1, l.size());
		}

		// L'index est vidé
		globalTiersIndexer.overwriteIndex();

		// L'index est vide
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(1111L);
			List<?> l = globalTiersSearcher.search(criteria);
			assertEquals(0, l.size());
		}

		// On index dabord avec 1 Thread pour mettre LOG_MDATE et INDEX_DIRTY comme il faut
		globalTiersIndexer.indexAllDatabaseAsync(null, 1, Mode.FULL, false);

		// Puis avec 4 pour vérifier que le multi-threading marche bien
		globalTiersIndexer.indexAllDatabaseAsync(null, 4, Mode.FULL, false);

		// De nouveau trouvé
		{
			long id = 7823L;

			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(id);
			List<?> l = globalTiersSearcher.search(criteria);
			assertEquals(1, l.size());

			Tiers tiers = tiersDAO.get(id);
			assertFalse(tiers.isDirty());
		}

		// Tiers non trouvé.
		{
			long id = 12345678L;

			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(id);
			List<?> l = globalTiersSearcher.search(criteria);
			assertEquals(0, l.size());

			Tiers tiers = tiersDAO.get(id);
			assertTrue(tiers.isDirty());
		}

		// Nombre de tiers indexés
		{
			final Set<Long> ids = globalTiersSearcher.getAllIds();
			assertTrue(ids.contains(Long.valueOf(1234)));
			assertTrue(ids.contains(Long.valueOf(5434)));
			assertTrue(ids.contains(Long.valueOf(7239)));
			assertTrue(ids.contains(Long.valueOf(7632)));
			assertTrue(ids.contains(Long.valueOf(7823)));
			assertTrue(ids.contains(Long.valueOf(8901)));
			assertTrue(ids.contains(Long.valueOf(27769)));
			assertTrue(ids.contains(Long.valueOf(76327)));

			int nb = globalTiersSearcher.getExactDocCount();
			assertEquals(8, nb);
		}
	}

}

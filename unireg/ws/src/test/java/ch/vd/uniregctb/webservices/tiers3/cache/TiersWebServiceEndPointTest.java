package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceSecurite;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersResponse;
import ch.vd.uniregctb.webservices.tiers3.TiersInfo;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.UserLogin;
import ch.vd.uniregctb.webservices.tiers3.impl.TiersWebServiceEndPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class TiersWebServiceEndPointTest extends WebserviceTest {

	private TiersWebServiceEndPoint endpoint;

	public TiersWebServiceEndPointTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		TiersWebService tiersService = getBean(TiersWebService.class, "tiersService3Impl");
		endpoint = new TiersWebServiceEndPoint();
		endpoint.setService(tiersService);
	}

	/**
	 * [UNIREG-1246] Teste que les opérateurs avec visualisation limitées peuvent accèder à la méthode searchTiers.
	 */
	@Test
	public void testVisualisationLimiteeSearchTiersRequest() throws Exception {

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur("test", 1234, Role.VISU_LIMITE.getIfosecCode());
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				return null;
			}
		});

		globalTiersIndexer.sync();

		SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(new UserLogin("test", 33));
		params.setNomCourrier("Jojo");

		// cet appel doit réussir
		final SearchTiersResponse reponse = endpoint.searchTiers(params);
		assertNotNull(reponse);

		final List<TiersInfo> results = reponse.getItem();
		assertNotNull(results);
		assertEquals(1, results.size());

		final TiersInfo info = results.get(0);
		assertNotNull(info);
		assertEquals("Jojo Leproux", info.getNom1());
	}

	/**
	 * Teste que la méthode getBatch fonctionne même avec des ids nuls.
	 */
	@Test
	public void testGetBatchTiersRequestWithIdNull() throws Exception {

		serviceSecurite.setUp(new DefaultMockServiceSecurite());
		serviceCivil.setUp(new DefaultMockServiceCivil());

		class Ids {
			long a;
			long b;
			long mc;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique a = addNonHabitant("A", "Bidon", date(1970, 4, 19), Sexe.MASCULIN);
				ids.a = a.getNumero();
				final PersonnePhysique b = addNonHabitant("B", "Bidon", date(1970, 4, 19), Sexe.FEMININ);
				ids.b = b.getNumero();
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(a, b, date(2000, 1, 1), null);
				ids.mc = ensemble.getMenage().getNumero();

				return null;
			}
		});

		final GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(new UserLogin("iamtestuser", 22));
		params.getTiersNumbers().add(ids.a);
		params.getTiersNumbers().add(ids.b);
		params.getTiersNumbers().add(null);

		BatchTiers results = endpoint.getBatchTiers(params);
		assertNotNull(results);
		final List<BatchTiersEntry> list = results.getEntries();
		assertEquals(2, list.size()); // dans la version 3 du web-service, les ids nuls sont ignorés

		Collections.sort(list, new Comparator<BatchTiersEntry>() {
			public int compare(BatchTiersEntry o1, BatchTiersEntry o2) {
				return Long.valueOf(o1.getNumber()).compareTo(o2.getNumber());
			}
		});

		assertEquals(ids.a, list.get(0).getNumber());
		assertEquals(ids.b, list.get(1).getNumber());
	}
}

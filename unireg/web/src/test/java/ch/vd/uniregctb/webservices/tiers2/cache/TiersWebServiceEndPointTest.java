package ch.vd.uniregctb.webservices.tiers2.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceSecurite;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.impl.TiersWebServiceEndPoint;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-webut-ws.xml"
	})
public class TiersWebServiceEndPointTest extends WebTest {

	private TiersWebServiceEndPoint endpoint;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		TiersWebService tiersService = getBean(TiersWebService.class, "tiersService2Bean");
		endpoint = new TiersWebServiceEndPoint();
		endpoint.setService(tiersService);
	}

	/**
	 * [UNIREG-1246] Teste que les opérateurs avec visualisation limitées peuvent accèder à la méthode searchTiers.
	 */
	@Test
	public void testVisualisationLimiteeSearchTiers() throws Exception {

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur("test", 1234, Role.VISU_LIMITE.getIfosecCode());
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				return null;
			}
		});

		SearchTiers params = new SearchTiers();
		params.login = new UserLogin("test", 33);
		params.nomCourrier= "Jojo";

		// cet appel doit réussir
		final List<TiersInfo> results = endpoint.searchTiers(params);
		assertNotNull(results);
		assertEquals(1, results.size());

		final TiersInfo info = results.get(0);
		assertNotNull(info);
		assertEquals("Leproux Jojo", info.nom1);
	}

	/**
	 * Teste que la méthode getBatch fonctionne même avec des ids nuls.
	 */
	@Test
	public void testGetBatchTiersWithIdNull() throws Exception {

		serviceSecurite.setUp(new DefaultMockServiceSecurite());
		serviceCivil.setUp(new DefaultMockServiceCivil());

		class Ids {
			long a;
			long b;
			long mc;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique a = addNonHabitant("A", "Bidon", date(1970, 4, 19), Sexe.MASCULIN);
				ids.a = a.getNumero();
				final PersonnePhysique b = addNonHabitant("B", "Bidon", date(1970, 4, 19), Sexe.FEMININ);
				ids.b = b.getNumero();
				final EnsembleTiersCouple ensemble = createEnsembleTiersCouple(a, b, date(2000, 1, 1));
				ids.mc = ensemble.getMenage().getNumero();

				return null;
			}
		});

		final GetBatchTiers params = new GetBatchTiers();
		params.login = new UserLogin("iamtestuser", 22);
		params.date=null;
		params.tiersNumbers = new HashSet<Long>();
		params.tiersNumbers.add(ids.a);
		params.tiersNumbers.add(ids.b);
		params.tiersNumbers.add(null);

		BatchTiers results = endpoint.getBatchTiers(params);
		assertNotNull(results);
		final List<BatchTiersEntry> list = results.entries;
		assertEquals(3, list.size());

		Collections.sort(list, new Comparator<BatchTiersEntry>() {
			public int compare(BatchTiersEntry o1, BatchTiersEntry o2) {
				if (o1.number == null) {
					return -1;
				}
				if (o2.number == null) {
					return 1;
				}
				return o1.number.compareTo(o2.number);
			}
		});

		assertNull(list.get(0).number);
		assertEquals(Long.valueOf(ids.a), list.get(1).number);
		assertEquals(Long.valueOf(ids.b), list.get(2).number);
	}
}

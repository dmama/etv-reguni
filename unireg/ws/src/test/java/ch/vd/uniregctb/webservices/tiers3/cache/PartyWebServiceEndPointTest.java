package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.tiers3.BatchParty;
import ch.vd.unireg.webservices.tiers3.BatchPartyEntry;
import ch.vd.unireg.webservices.tiers3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.tiers3.PartyWebService;
import ch.vd.unireg.webservices.tiers3.SearchPartyRequest;
import ch.vd.unireg.webservices.tiers3.SearchPartyResponse;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceSecurite;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.webservices.tiers3.impl.PartyWebServiceEndPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class PartyWebServiceEndPointTest extends WebserviceTest {

	private PartyWebServiceEndPoint endpoint;

	public PartyWebServiceEndPointTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		PartyWebService partyService = getBean(PartyWebService.class, "partyService3Impl");
		endpoint = new PartyWebServiceEndPoint();
		endpoint.setService(partyService);
	}

	/**
	 * [UNIREG-1246] Teste que les opérateurs avec visualisation limitées peuvent accèder à la méthode searchParty.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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

		SearchPartyRequest params = new SearchPartyRequest();
		params.setLogin(new UserLogin("test", 33));
		params.setContactName("Jojo");

		// cet appel doit réussir
		final SearchPartyResponse reponse = endpoint.searchParty(params);
		assertNotNull(reponse);

		final List<PartyInfo> results = reponse.getItems();
		assertNotNull(results);
		assertEquals(1, results.size());

		final PartyInfo info = results.get(0);
		assertNotNull(info);
		assertEquals("Jojo Leproux", info.getName1());
	}

	/**
	 * Teste que la méthode getBatch fonctionne même avec des ids nuls.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(new UserLogin("iamtestuser", 22));
		params.getPartyNumbers().add((int) ids.a);
		params.getPartyNumbers().add((int) ids.b);
		params.getPartyNumbers().add(null);

		BatchParty results = endpoint.getBatchParty(params);
		assertNotNull(results);
		final List<BatchPartyEntry> list = results.getEntries();
		assertEquals(2, list.size()); // dans la version 3 du web-service, les ids nuls sont ignorés

		Collections.sort(list, new Comparator<BatchPartyEntry>() {
			@Override
			public int compare(BatchPartyEntry o1, BatchPartyEntry o2) {
				return Integer.valueOf(o1.getNumber()).compareTo(o2.getNumber());
			}
		});

		assertEquals(ids.a, list.get(0).getNumber());
		assertEquals(ids.b, list.get(1).getNumber());
	}
}

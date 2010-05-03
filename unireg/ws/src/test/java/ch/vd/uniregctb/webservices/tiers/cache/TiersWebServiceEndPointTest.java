package ch.vd.uniregctb.webservices.tiers.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.TiersWebService;
import ch.vd.uniregctb.webservices.tiers.impl.TiersWebServiceEndPoint;
import ch.vd.uniregctb.webservices.tiers.params.SearchTiers;

public class TiersWebServiceEndPointTest extends WebserviceTest {

	private TiersWebServiceEndPoint endpoint;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		TiersWebService tiersService = getBean(TiersWebService.class, "tiersServiceBean");
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

}

package ch.vd.uniregctb.interfaces;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class ServiceSecuriteTest extends BusinessItTest {

	private ServiceSecuriteService service;

	// private static final Logger LOGGER = Logger.getLogger(ServiceSecuriteTest.class);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceSecuriteService.class, "serviceSecuriteService");
	}

	@Test
	public void testGetCollectivitesUtilisateur() throws Exception {
		final List<?> collectivites = service.getCollectivitesUtilisateur("ZAIPTF");
		assertNotNull(collectivites);
		assertTrue(collectivites.size() >= 1);
	}

}

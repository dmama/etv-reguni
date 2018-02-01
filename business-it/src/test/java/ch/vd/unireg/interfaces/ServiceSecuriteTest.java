package ch.vd.unireg.interfaces;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServiceSecuriteTest extends BusinessItTest {

	private ServiceSecuriteService service;

	// private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSecuriteTest.class);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceSecuriteService.class, "serviceSecuriteService");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetCollectivitesUtilisateur() throws Exception {
		final List<?> collectivites = service.getCollectivitesUtilisateur("ZAIPTF");
		assertNotNull(collectivites);
		assertTrue(collectivites.size() >= 1);
	}

}

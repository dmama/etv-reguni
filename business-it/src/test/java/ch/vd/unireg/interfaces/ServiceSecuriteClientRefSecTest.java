package ch.vd.unireg.interfaces;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.ProfileOperateur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServiceSecuriteClientRefSecTest extends BusinessItTest {

	private ServiceSecuriteService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(ServiceSecuriteService.class, "serviceSecuriteClientRefSec");
	}

	@Test
	public void testGetCollectivitesUtilisateur() throws Exception {
		final List<?> collectivites = service.getCollectivitesUtilisateur("ZAIZZT");
		assertNotNull(collectivites);
		assertTrue(collectivites.size() >= 1);
	}

	@Test
	public void testGetProfileUtilisateurCollectivite_22() throws Exception {
		final ProfileOperateur profileUtilisateur = service.getProfileUtilisateur("ZAIZZT", 22);
		assertNotNull(profileUtilisateur);
		assertTrue(profileUtilisateur.getProcedures().size() > 10);
		assertNotNull(profileUtilisateur.getNoTelephone());
	}

	@Test
	public void testGetProfileUtilisateurCollectiviteInexistante() throws Exception {
		final ProfileOperateur profileUtilisateur = service.getProfileUtilisateur("ZAIZZT", 0);
		assertNotNull(profileUtilisateur);
		assertEquals(0, profileUtilisateur.getProcedures().size());
		assertNull(profileUtilisateur.getNoTelephone());
	}

}

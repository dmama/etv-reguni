package ch.vd.unireg.interfaces;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.ProcedureSecurite;
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
		assertEquals(1, collectivites.size());
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
		assertNull(profileUtilisateur);
	}

	/**
	 * [SIFISC-30975] Ce test vérifie que les procédures sont bien triées par ordre alphabétique croissant des codes.
	 */
	@Test
	public void testGetProfileUtilisateurZaidra() throws Exception {
		final ProfileOperateur profile = service.getProfileUtilisateur("zaidra", 22);
		assertNotNull(profile);

		final List<ProcedureSecurite> sorted = profile.getProcedures().stream()
				.sorted(Comparator.comparing(ProcedureSecurite::getCode))
				.collect(Collectors.toList());
		assertEquals(sorted, profile.getProcedures());
	}
}

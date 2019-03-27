package ch.vd.unireg.webservice.refsec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.wsclient.refsec.RefSecClient;
import ch.vd.unireg.wsclient.refsec.model.Procedure;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;
import ch.vd.unireg.wsclient.refsec.model.User;

import static ch.vd.unireg.webservice.rcent.RcEntClientItTest.TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RefSecClientItTest extends BusinessItTest {

	private RefSecClient refSecClient;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		refSecClient = getBean(RefSecClient.class, "refSecClient");
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

	@Test(timeout = TIMEOUT)
	public void testPofileRefSec() throws Exception {
		final ProfilOperateur profile = refSecClient.getProfilOperateur("ZAIZZT", 22);
		assertNotNull(profile);
		final Set<Procedure> procedures = profile.getProcedures();
		assertTrue(!procedures.isEmpty());
		assertTrue(procedures.contains(new Procedure("UR000002", null)));
		assertTrue(procedures.contains(new Procedure("UR000073", null)));
		assertTrue(procedures.contains(new Procedure("UR000094", null)));
		assertTrue(procedures.contains(new Procedure("UR000171", null)));
	}

	@Test(timeout = TIMEOUT)
	public void testPofileOperateurInconnu() throws Exception {
		final ProfilOperateur profile = refSecClient.getProfilOperateur("zaifpt", 22);
		assertNull(profile);
	}

	@Test(timeout = TIMEOUT)
	public void testGetUser() throws Exception {
		final User user = refSecClient.getUser("Usrreg20");
		assertNotNull(user);
		assertEquals("Usrreg20", user.getFirstName());
		assertEquals("Generic", user.getLastName());
		assertEquals("noreply@vd.ch", user.getEmail());
	}

	@Test(timeout = TIMEOUT)
	public void testGetCollectiviteUtilisateur() throws Exception {
		final Set<Integer> ids = refSecClient.getCollectivitesOperateur("zaidra");
		assertNotNull(ids);
		assertEquals(new HashSet<>(Arrays.asList(1, 5, 7, 8, 12, 16, 18, 19, 21, 22)), ids);
	}

	@Test(timeout = TIMEOUT)
	public void testPing() throws Exception {
		refSecClient.ping();
	}
}

package ch.vd.unireg.webservice.refsec;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.wsclient.refsec.ClientRefSec;
import ch.vd.unireg.wsclient.refsec.model.RefSecProfilOperateur;

import static ch.vd.unireg.webservice.rcent.RcEntClientItTest.TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RefSecClientItTest extends BusinessItTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(RefSecClientItTest.class);
	private ClientRefSec clientRefSec;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		clientRefSec = getBean(ClientRefSec.class, "refSecClient");
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}


	@Test(timeout = TIMEOUT)
	public void testPofileRefSec() throws Exception {
		final List<RefSecProfilOperateur> profiles = clientRefSec.getProfileUtilisateurs("ZAIZZT");
		assertNotNull(profiles);
		assertEquals(profiles.size(), 4);
	}

	@Test(timeout = TIMEOUT)
	public void testPing() throws Exception {
		final String pong = clientRefSec.ping();
		assertNotNull(pong);
		final String aux = "{" + '"' + "status" + '"' + ":" + '"' + "UP" + '"' + "}";
		assertEquals(pong, aux);
	}

}

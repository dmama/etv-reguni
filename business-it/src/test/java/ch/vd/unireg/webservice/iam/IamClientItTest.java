package ch.vd.unireg.webservice.iam;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.wsclient.iam.IamClient;
import ch.vd.unireg.wsclient.iam.IamUser;

import static ch.vd.unireg.webservice.rcent.RcEntClientItTest.TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IamClientItTest extends BusinessItTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(IamClientItTest.class);
	private IamClient iamClient;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		iamClient = getBean(IamClient.class, "iamClient");
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}


	@Test(timeout = TIMEOUT)
	public void testUtilisateurIam() throws Exception {
		final IamUser iamUser = iamClient.getUser("ZAIZZT");
		assertNotNull(iamUser);
		assertEquals(iamUser.getIamId(),"ZAIZZT");
	}

}

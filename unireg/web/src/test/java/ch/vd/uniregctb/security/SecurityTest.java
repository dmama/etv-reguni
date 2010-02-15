package ch.vd.uniregctb.security;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;

public abstract class SecurityTest extends WebTest {

	protected static final String TEST_OP_NAME = "test";
	protected static final int TEST_OP_NO_IND = 1234;

	protected void setupDefaultTestOperateur() {
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(TEST_OP_NAME, TEST_OP_NO_IND, Role.VISU_ALL.getIfosecCode());
			}
		});
	}
}

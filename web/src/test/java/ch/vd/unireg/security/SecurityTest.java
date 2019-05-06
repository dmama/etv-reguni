package ch.vd.unireg.security;

import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.interfaces.service.mock.MockServiceSecuriteService;

public abstract class SecurityTest extends WebTest {

	protected static final String TEST_OP_NAME = "test";

	protected void setupDefaultTestOperateur() {
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(TEST_OP_NAME, Role.VISU_ALL);
			}
		});
	}
}

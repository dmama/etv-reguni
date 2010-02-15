package ch.vd.uniregctb.couple;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractCoupleControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "classpath:ch/vd/uniregctb/couple/AbstractCoupleControllerTest.xml";

	protected TiersDAO tiersDAO;

	protected Long numeroPP1 = 12300002L;
	protected Long numeroPP2 = 12300003L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		loadDatabase(DB_UNIT_FILE);
	}

}

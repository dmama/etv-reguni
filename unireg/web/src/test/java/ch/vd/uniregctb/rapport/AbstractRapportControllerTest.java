package ch.vd.uniregctb.rapport;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractRapportControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractRapportControllerTest.xml";

	protected TiersDAO tiersDAO;

	protected Long numeroTiers = 12300002L;
	protected Long numeroTiersLie = 12300003L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		loadDatabase(DB_UNIT_FILE);
	}

}
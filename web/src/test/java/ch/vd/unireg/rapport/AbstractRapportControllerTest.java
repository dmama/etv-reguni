package ch.vd.unireg.rapport;

import ch.vd.unireg.common.WebTest;

public abstract class AbstractRapportControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private static final String DB_UNIT_FILE = "AbstractRapportControllerTest.xml";

	protected Long numeroTiers = 12300002L;
	protected Long numeroTiersLie = 12300003L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		loadDatabase(DB_UNIT_FILE);
	}

}
package ch.vd.uniregctb.tache;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.TacheDAO;

public abstract class AbstractTacheControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractTacheControllerTest.xml";

	protected TacheDAO tacheDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");

		loadDatabase(DB_UNIT_FILE);
	}

}
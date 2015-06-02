package ch.vd.uniregctb.lr;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;

public abstract class AbstractLrControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractLrControllerTest.xml";

	protected ListeRecapitulativeDAO lrDAO;

	protected Long idLR1 = 21L;
	protected Long idLR2 = 22L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrDAO = getBean(ListeRecapitulativeDAO.class, "lrDAO");

		loadDatabase(DB_UNIT_FILE);
	}

}

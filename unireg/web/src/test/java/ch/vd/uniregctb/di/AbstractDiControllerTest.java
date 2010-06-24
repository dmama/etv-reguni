package ch.vd.uniregctb.di;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;

public abstract class AbstractDiControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractDiControllerTest.xml";

	protected DeclarationImpotOrdinaireDAO diDAO;

	protected Long idDI1 = 210L;
	protected Long idDI2 = 220L;
	protected Long numero = 12300002L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");

		loadDatabase(DB_UNIT_FILE);
	}

}
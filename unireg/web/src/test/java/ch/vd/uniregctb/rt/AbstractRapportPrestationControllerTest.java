package ch.vd.uniregctb.rt;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractRapportPrestationControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractRapportPrestationControllerTest.xml";

	protected Long numeroSrc = 12300003L;
	protected Long numeroDpi = 12500001L;

	protected String PROVENANCE_SOURCIER = "sourcier";
	protected String PROVENANCE_DEBITEUR = "debiteur";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		loadDatabase(DB_UNIT_FILE);
	}

}

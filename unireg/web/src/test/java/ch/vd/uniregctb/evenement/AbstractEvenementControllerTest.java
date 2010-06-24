package ch.vd.uniregctb.evenement;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractEvenementControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractEvenementControllerTest.xml";

	protected TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				Individu individu = addIndividu(333908, RegDate.get(1974, 3, 22), "Schmidt", "Laurent", true);

				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		loadDatabase(DB_UNIT_FILE);
	}


}
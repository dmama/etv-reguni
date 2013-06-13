package ch.vd.uniregctb.fusion;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public abstract class AbstractFusionControllerTest extends WebTest {

	/**
	 * DB unit
	 */
	private final static String DB_UNIT_FILE = "AbstractFusionControllerTest.xml";

	protected Long numeroNonHab = 32100002L;
	protected Long numeroHab = 12300002L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu = addIndividu(333908, RegDate.get(1974, 3, 22), "Schmidt", "Laurent", true);

				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
			}
		});

		loadDatabase(DB_UNIT_FILE);
	}


}


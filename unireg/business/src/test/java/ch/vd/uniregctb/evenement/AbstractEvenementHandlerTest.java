package ch.vd.uniregctb.evenement;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandler;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.MockEvenementFiscalSender;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class AbstractEvenementHandlerTest extends BusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(AbstractEvenementHandlerTest.class);

	/**
	 * Une instance du gestionnaire d'événement.
	 */
	protected EvenementCivilHandler evenementCivilHandler;

	protected TiersDAO tiersDAO;
	protected ProxyServiceCivil serviceCivil;
	protected GlobalTiersIndexer indexer;
	protected MockEvenementFiscalSender eventSender;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		evenementCivilHandler = getBean(EvenementCivilHandler.class, getHandlerBeanName());
		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
		indexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		eventSender = getBean(MockEvenementFiscalSender.class, "evenementFiscalSender");
		eventSender.count = 0;
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
		if (serviceCivil != null) {
			serviceCivil.tearDown();
		}
	}

	public String getHandlerBeanName() {

		String cn = getClass().getSimpleName();
		String first = cn.substring(0, 1); // Premiere lettre
		first = first.toLowerCase(); // En minuscule
		cn = cn.substring(1, cn.length()-4); // Suppression de "Test"
		cn = first + cn;
		return cn;
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return getBean(EvenementFiscalService.class,"evenementFiscalService");
	}
}

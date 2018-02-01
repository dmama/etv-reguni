package ch.vd.unireg.evenement.ide.service;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.ide.AnnonceIDEServiceImpl;
import ch.vd.unireg.evenement.ide.ServiceIDEService;

/**
 * @author Raphaël Marmier, 2016-09-14, <raphael.marmier@vd.ch>
 */
public abstract class AbstractServiceIDEServiceTest extends BusinessTest {

	protected ServiceIDEService serviceIDE;
	protected AnnonceIDEServiceImpl annonceIDEService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceIDE = getBean(ServiceIDEService.class, "serviceIDEService");
		annonceIDEService = getBean(AnnonceIDEServiceImpl.class, "annonceIDEService");
	}
}
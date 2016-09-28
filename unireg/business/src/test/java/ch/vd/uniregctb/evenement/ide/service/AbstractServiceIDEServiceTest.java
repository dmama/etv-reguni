package ch.vd.uniregctb.evenement.ide.service;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.ide.AnnonceIDEServiceImpl;
import ch.vd.uniregctb.evenement.ide.ServiceIDEService;

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
package ch.vd.uniregctb.interfaces;

import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class ServiceCivilHostInterfacesTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceCivilService.class, "serviceCivilServiceHost");
	}
}

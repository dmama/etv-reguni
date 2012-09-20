package ch.vd.uniregctb.interfaces;

import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.uniregctb.interfaces.service.ServiceCivilImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class ServiceCivilHostInterfacesTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final ServiceCivilRaw raw = getBean(ServiceCivilRaw.class, "serviceCivilServiceHost");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		service = new ServiceCivilImpl(infraService, raw);
	}
}

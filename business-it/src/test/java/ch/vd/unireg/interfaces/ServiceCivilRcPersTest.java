package ch.vd.unireg.interfaces;

import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class ServiceCivilRcPersTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final ServiceCivilRaw raw = getBean(ServiceCivilRaw.class, "serviceCivilRcPers");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		service = new ServiceCivilImpl(infraService, raw);
	}
}

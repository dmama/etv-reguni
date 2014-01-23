package ch.vd.uniregctb.interfaces;

import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.uniregctb.interfaces.service.ServiceCivilImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class ServiceCivilRcPersTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final ServiceCivilRaw raw = getBean(ServiceCivilRaw.class, "serviceCivilRcPers");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		service = new ServiceCivilImpl(infraService, raw);
	}
}

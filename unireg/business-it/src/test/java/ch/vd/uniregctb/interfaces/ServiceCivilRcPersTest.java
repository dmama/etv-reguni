package ch.vd.uniregctb.interfaces;

import org.junit.Ignore;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-rcpers.xml"
})
@Ignore
public class ServiceCivilRcPersTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(ServiceCivilService.class, "serviceCivilRcPers");
	}
}

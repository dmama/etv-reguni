package ch.vd.uniregctb.common;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServicePM;

@ContextConfiguration(locations = {
	BusinessTestingConstants.UNIREG_BUSINESS_UT_JOBS
})
public abstract class BusinessTest extends AbstractBusinessTest {

	// private final static Logger LOGGER = Logger.getLogger(BusinessTest.class);

	protected ProxyServiceCivil serviceCivil;
	protected ProxyServicePM servicePM;
	protected ProxyServiceInfrastructureService serviceInfra;

	@Override
	protected void runOnSetUp() throws Exception {

		try {
			serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
			servicePM = getBean(ProxyServicePM.class, "servicePersonneMoraleService");
			serviceInfra = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
			serviceInfra.setUpDefault();

			super.runOnSetUp();
		}
		catch (Exception e) {
			serviceCivil.tearDown();
			servicePM.tearDown();
			serviceInfra.tearDown();
			throw e;
		}
		catch (Throwable t) {
			serviceCivil.tearDown();
			servicePM.tearDown();
			serviceInfra.tearDown();
			throw new Exception(t);
		}
	}

	@Override
	public void onTearDown() throws Exception {

		try {
			super.onTearDown();
		}
		finally {
			/*
			 * Il faut l'enlever apres le onTearDown parce que le endTransaction en a besoin pour faire l'indexation lors du commit()
			 */
			serviceCivil.tearDown();
			servicePM.tearDown();
		}
	}

	@Override
	protected void loadDatabase(String filename) throws Exception {
		try {
			super.loadDatabase(filename);
		}
		catch (Exception e) {
			serviceCivil.tearDown();
			servicePM.tearDown();
			throw e;
		}
	}
}

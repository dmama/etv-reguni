package ch.vd.uniregctb.common;

import java.util.Date;

import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServicePM;
import ch.vd.uniregctb.scheduler.JobDefinition;

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
			if (serviceCivil != null) {
				serviceCivil.tearDown();
			}
			if (servicePM != null) {
				servicePM.tearDown();
			}
			if (serviceInfra != null) {
				serviceInfra.tearDown();
			}
			throw e;
		}
		catch (Throwable t) {
			if (serviceCivil != null) {
				serviceCivil.tearDown();
			}
			if (servicePM != null) {
				servicePM.tearDown();
			}
			if (serviceInfra != null) {
				serviceInfra.tearDown();
			}
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

	protected static void waitUntilRunning(JobDefinition job, Date startTime) throws Exception {
		while (job.getLastStart() == null || job.getLastStart().before(startTime)) {
			Thread.sleep(100); // 100ms
		}
	}

	protected static interface IndividuModification {
		void modifyIndividu(MockIndividu individu);
	}

	protected void doModificationIndividu(long noIndividu, IndividuModification modifier) {
		final MockIndividu ind = ((MockServiceCivil) serviceCivil.getTarget()).getIndividu(noIndividu);
		modifier.modifyIndividu(ind);
	}
}

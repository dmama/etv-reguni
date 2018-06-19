package ch.vd.unireg.common;

import java.util.Date;

import ch.vd.unireg.evenement.ide.AnnonceIDEService;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceEntreprise;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.scheduler.JobDefinition;

public abstract class BusinessTest extends AbstractBusinessTest {

	// private static final Logger LOGGER = LoggerFactory.getLogger(BusinessTest.class);

	protected ProxyServiceCivil serviceCivil;
	protected ProxyServiceEntreprise serviceEntreprise;
	protected ProxyServiceInfrastructureService serviceInfra;
	protected AnnonceIDEService annonceIDEService;
	protected RegimeFiscalService regimeFiscalService;

	@Override
	public void onSetUp() throws Exception {

		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
		serviceEntreprise = getBean(ProxyServiceEntreprise.class, "serviceEntreprise");
		serviceInfra = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
		serviceInfra.setUpDefault();

		super.onSetUp();
	}

	protected static void waitUntilRunning(JobDefinition job, Date startTime) throws Exception {
		while (job.getLastStart() == null || job.getLastStart().before(startTime)) {
			Thread.sleep(100); // 100ms
		}
	}

	protected interface IndividuModification {
		void modifyIndividu(MockIndividu individu);
	}

	protected interface IndividusModification {
		void modifyIndividus(MockIndividu individu, MockIndividu other);
	}

	protected void doModificationIndividu(long noIndividu, IndividuModification modifier) {
		final MockIndividu ind = ((MockServiceCivil) serviceCivil.getUltimateTarget()).getIndividu(noIndividu);
		modifier.modifyIndividu(ind);
	}

	protected void doModificationIndividus(long noIndividu, long noOther, IndividusModification modifier) {
		final MockIndividu ind = ((MockServiceCivil) serviceCivil.getUltimateTarget()).getIndividu(noIndividu);
		final MockIndividu other = ((MockServiceCivil) serviceCivil.getUltimateTarget()).getIndividu(noOther);
		modifier.modifyIndividus(ind, other);
	}
}

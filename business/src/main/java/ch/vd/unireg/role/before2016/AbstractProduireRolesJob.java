package ch.vd.unireg.role.before2016;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public abstract class AbstractProduireRolesJob extends JobDefinition {

	private RoleService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_THREADS = "NB_THREADS";

	protected static JobParam createParamPeriodeFiscale() {
		final JobParam param = new JobParam();
		param.setDescription("Période fiscale");
		param.setName(PERIODE_FISCALE);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		return param;
	}

	protected static JobParam createParamNbThreads() {
		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		return param;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}

	protected AbstractProduireRolesJob(String name, JobCategory categorie, int sortOrder, String description) {
		super(name, categorie, sortOrder, description);
	}

	public void setService(RoleService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	protected RoleService getService() {
		return service;
	}

	protected RapportService getRapportService() {
		return rapportService;
	}

	protected PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	protected final int getPeriodeFiscale(Map<String, Object> params) {
		return getIntegerValue(params, PERIODE_FISCALE);
	}

	protected final int getNbThreads(Map<String, Object> params) {
		return getStrictlyPositiveIntegerValue(params, NB_THREADS);
	}
}

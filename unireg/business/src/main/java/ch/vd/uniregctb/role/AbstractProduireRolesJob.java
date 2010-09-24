package ch.vd.uniregctb.role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

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

	protected AbstractProduireRolesJob(String name, String categorie, int sortOrder, String description, List<JobParam> paramsDef, HashMap<String, Object> defaultParams) {
		super(name, categorie, sortOrder, description, paramsDef, defaultParams);
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

	protected static int getPeriodeFiscale(Map<String, Object> params) {
		final Integer pf = (Integer) params.get(PERIODE_FISCALE);
		if (pf == null) {
			throw new RuntimeException("La période fiscale doit être spécifiée.");
		}
		return pf;
	}

	protected static int getNbThreads(Map<String, Object> params) {
		final Integer nbThreads = (Integer) params.get(NB_THREADS);
		if (nbThreads == null) {
			throw new RuntimeException("Le nombre de threads doit être spécifié.");
		}
		return nbThreads;
	}
}

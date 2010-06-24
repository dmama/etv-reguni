package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.MajoriteRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class OuvertureForsContribuablesMajeursJob extends JobDefinition {

	public static final String NAME = "OuvertureForsContribuableMajeurJob";
	private static final String CATEGORIE = "Fors";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	private PlatformTransactionManager transactionManager;
	private MetierService metierService;
	private RapportService rapportService;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param2 = new JobParam();
			param2.setDescription("Date de traitement");
			param2.setName(DATE_TRAITEMENT);
			param2.setMandatory(false);
			param2.setType(new JobParamRegDate());
			params.add(param2);
		}

		defaultParams = new HashMap<String, Object>();
		{
			//RegDate today = RegDate.get();
			//defaultParams.put(DATE_TRAITEMENT, RegDateHelper.dateToDashString(today));
		}
	}

	public OuvertureForsContribuablesMajeursJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public OuvertureForsContribuablesMajeursJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		params.get(0).setEnabled(isTesting());
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job en dehors d'une transaction, parce qu'il la gère lui-même
		final OuvertureForsResults results = metierService.ouvertureForsContribuablesMajeurs(dateTraitement, getStatusManager());

		// Exécution du rapport dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		MajoriteRapport rapport = (MajoriteRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		setLastRunReport(rapport);
		Audit.success("L'ouverture des fors des habitants majeurs à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

}

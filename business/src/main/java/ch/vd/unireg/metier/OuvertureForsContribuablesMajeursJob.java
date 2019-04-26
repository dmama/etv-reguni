package ch.vd.unireg.metier;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.document.MajoriteRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class OuvertureForsContribuablesMajeursJob extends JobDefinition {

	public static final String NAME = "OuvertureForsContribuableMajeurJob";

	private PlatformTransactionManager transactionManager;
	private MetierService metierService;
	private RapportService rapportService;

	public OuvertureForsContribuablesMajeursJob(int sortOrder, String description) {
		super(NAME, JobCategory.FORS, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Date de traitement");
		param.setName(DATE_TRAITEMENT);
		param.setMandatory(false);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
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
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job en dehors d'une transaction, parce qu'il la gère lui-même
		final OuvertureForsResults results = metierService.ouvertureForsContribuablesMajeurs(dateTraitement, getStatusManager());

		// Exécution du rapport dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		MajoriteRapport rapport = template.execute(new TransactionCallback<MajoriteRapport>() {
			@Override
			public MajoriteRapport doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		setLastRunReport(rapport);
		audit.success("L'ouverture des fors des habitants majeurs à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

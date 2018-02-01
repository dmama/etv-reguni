package ch.vd.uniregctb.metier;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.PassageNouveauxRentiersSourciersEnMixteRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class PassageNouveauxRentiersSourciersEnMixteJob extends JobDefinition {

	public static final String NAME = "PassageNouveauxRentiersSourciersEnMixteJob";

	private PlatformTransactionManager transactionManager;
	private MetierService metierService;
	private RapportService rapportService;

	public PassageNouveauxRentiersSourciersEnMixteJob(int sortOrder, String description) {
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
		final PassageNouveauxRentiersSourciersEnMixteResults results = metierService.passageSourcierEnMixteNouveauxRentiers(dateTraitement, getStatusManager());

		// Exécution du rapport dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		PassageNouveauxRentiersSourciersEnMixteRapport rapport = template.execute(new TransactionCallback<PassageNouveauxRentiersSourciersEnMixteRapport>() {
			@Override
			public PassageNouveauxRentiersSourciersEnMixteRapport doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		setLastRunReport(rapport);
		Audit.success("Le passage des nouveaux rentiers de sourcier à mixte 1 en date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

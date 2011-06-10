package ch.vd.uniregctb.mouvement;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DeterminerMouvementsDossiersEnMasseRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class DeterminerMouvementsDossiersEnMasseJob extends JobDefinition {

	public static final String NAME = "DeterminerMouvementsDossiersEnMasseJob";

	private static final String CATEGORIE = "Tiers";

	private static final String ARCHIVES_SEULEMENT = "ARCHIVES_SEULEMENT";

	private MouvementService mouvementService;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	public DeterminerMouvementsDossiersEnMasseJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Archives seulement");
			param.setName(ARCHIVES_SEULEMENT);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementService(MouvementService mouvementService) {
		this.mouvementService = mouvementService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);
		final boolean archivesSeulement = getBooleanValue(params, ARCHIVES_SEULEMENT);

		final StatusManager statusManager = getStatusManager();
		final DeterminerMouvementsDossiersEnMasseResults results = mouvementService.traiteDeterminationMouvements(dateTraitement, archivesSeulement, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final DeterminerMouvementsDossiersEnMasseRapport rapport = template.execute(new TransactionCallback<DeterminerMouvementsDossiersEnMasseRapport>() {
			@Override
			public DeterminerMouvementsDossiersEnMasseRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("La détermination des mouvements de dossiers en masse pour l'année %d au %s est terminée",
				dateTraitement.year() - 1, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}

}

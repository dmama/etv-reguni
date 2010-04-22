package ch.vd.uniregctb.mouvement;

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
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DeterminerMouvementsDossiersEnMasseRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class DeterminerMouvementsDossiersEnMasseJob extends JobDefinition {

	public static final String NAME = "DeterminerMouvementsDossiersEnMasseJob";

	private static final String CATEGORIE = "Tiers";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	private MouvementService mouvementService;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	static {

		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
	}

	public DeterminerMouvementsDossiersEnMasseJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public DeterminerMouvementsDossiersEnMasseJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	public void setMouvementService(MouvementService mouvementService) {
		this.mouvementService = mouvementService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		params.get(0).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);

		final StatusManager statusManager = getStatusManager();
		final DeterminerMouvementsDossiersEnMasseResults results = mouvementService.traiteDeterminationMouvements(dateTraitement, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final DeterminerMouvementsDossiersEnMasseRapport rapport = (DeterminerMouvementsDossiersEnMasseRapport) template.execute(new TransactionCallback() {
			public DeterminerMouvementsDossiersEnMasseRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("La détermination des mouvements de dossiers en masse pour l'année %d au %s est terminée",
				dateTraitement.year() - 1, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}

}

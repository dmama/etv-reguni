package ch.vd.uniregctb.listes.afc.pm;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.ExtractionDonneesRptRapport;
import ch.vd.uniregctb.listes.afc.ExtractionDonneesRptService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job d'extraction des listes de contribuables PM pour l'AFC (administration fédérale des contributions)
 */
public class ExtractionDonneesRptPMJob extends JobDefinition {

	public static final String NAME = "ExtractionDonneesRptPMJob";

	public static final String NB_THREADS = "NB_THREADS";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String VERSION_WS = "VERSION_WS";

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	private ExtractionDonneesRptService service;

	public ExtractionDonneesRptPMJob(int order, String description) {
		super(NAME, JobCategory.STATS, order, description);

		{
			final RegDate today = RegDate.get();
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, today.year() - 2);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Version des énumérations exposées (idem WS)");
			param.setName(VERSION_WS);
			param.setType(new JobParamEnum(VersionWS.class));
			param.setMandatory(true);
			addParameterDefinition(param, VersionWS.V7);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setService(ExtractionDonneesRptService service) {
		this.service = service;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère les paramètres
		final int nbThreads = getStrictlyPositiveIntegerValue(params,  NB_THREADS);
		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final VersionWS versionWS = getEnumValue(params, VERSION_WS, VersionWS.class);

		// on fait le boulot !
		final ExtractionDonneesRptPMResults results = service.produireExtractionIBC(dateTraitement, pf, versionWS, nbThreads, statusManager);

		// on génère un rapport
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ExtractionDonneesRptRapport rapport = template.execute(new TransactionCallback<ExtractionDonneesRptRapport>() {
			@Override
			public ExtractionDonneesRptRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("L'extraction des données de référence RPT PM IBC (%s %d) en date du %s est terminée.", null, pf, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

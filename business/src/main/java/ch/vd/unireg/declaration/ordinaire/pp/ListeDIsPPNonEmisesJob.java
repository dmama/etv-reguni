package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.Document;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public class ListeDIsPPNonEmisesJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	private ParametreAppService paramsApp;

	public static final String NAME = "ListeDINonEmisesJob";
	public static final String PERIODE_FISCALE = "PERIODE";

	public ListeDIsPPNonEmisesJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		RegDate today = RegDate.get();
		final JobParam param = new JobParam();
		param.setDescription("Période fiscale");
		param.setName(PERIODE_FISCALE);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, today.year() - 1);
	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		// Récupération des paramètres
		final int annee = getIntegerValue(params, PERIODE_FISCALE);
		if (annee >= RegDate.get().year()) {
			throw new RuntimeException("La période fiscale ne peut être postérieure ou égale à l'année en cours.");
		}
		if (annee < paramsApp.getPremierePeriodeFiscalePersonnesPhysiques()) {
			throw new RuntimeException("La période fiscale ne peut être antérieure à l'année " + paramsApp.getPremierePeriodeFiscalePersonnesPhysiques());
		}

		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui

		// Exécution de l'envoi dans une transaction.
		final ListeDIsPPNonEmises results = service.produireListeDIsNonEmises(annee, dateTraitement, getStatusManager());

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		final Document report = template.execute(new TransactionCallback<Document>() {
			@Override
			public Document doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		setLastRunReport(report);
		Audit.success("Liste DIs non émises générée correctement", report);
	}

	public void setParamsApp(ParametreAppService paramsApp) {
		this.paramsApp = paramsApp;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

package ch.vd.uniregctb.declaration;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ListeDIsNonEmises;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class ListeDINonEmisesJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	private ParametreAppService paramsApp;

	public static final String NAME = "ListeDINonEmisesJob";
	private static final String CATEGORIE = "Stats";

	public static final String PERIODE_FISCALE = "PERIODE";

	public ListeDINonEmisesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

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
		if (annee < paramsApp.getPremierePeriodeFiscale()) {
			throw new RuntimeException("La période fiscale ne peut être antérieure à l'année " + paramsApp.getPremierePeriodeFiscale());
		}

		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui

		// Exécution de l'envoi dans une transaction.
		final ListeDIsNonEmises results = service.produireListeDIsNonEmises(annee, dateTraitement, getStatusManager());

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

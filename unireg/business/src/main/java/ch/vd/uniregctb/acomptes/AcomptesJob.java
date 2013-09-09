package ch.vd.uniregctb.acomptes;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.AcomptesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class AcomptesJob  extends JobDefinition {

	public static final String NAME = "AcomptesJob";
	private static final String CATEGORIE = "Stats";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String PERIODE_FISCALE = "PERIODE";

	private AcomptesService service;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	public AcomptesJob(int order, String description) {
		super(NAME, CATEGORIE, order, description);

		final RegDate today = RegDate.get();
		final JobParam param0 = new JobParam();
		param0.setDescription("Période fiscale");
		param0.setName(PERIODE_FISCALE);
		param0.setMandatory(true);
		param0.setType(new JobParamInteger());
		addParameterDefinition(param0, today.year() + 1);

		final JobParam param1 = new JobParam();
		param1.setDescription("Nombre de threads");
		param1.setName(I_NB_THREADS);
		param1.setMandatory(true);
		param1.setType(new JobParamInteger());
		addParameterDefinition(param1, 2);
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setService(AcomptesService service) {
		this.service = service;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère le nombre de threads paramétrés
		final int nbThreads = getStrictlyPositiveIntegerValue(params, I_NB_THREADS);
		final int annee = getIntegerValue(params, PERIODE_FISCALE);

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final AcomptesResults results = service.produireAcomptes(dateTraitement, nbThreads, annee, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final AcomptesRapport rapport = template.execute(new TransactionCallback<AcomptesRapport>() {
			@Override
			public AcomptesRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des populations pour les bases acomptes en date du " + dateTraitement + " est terminée.", rapport);
	}

}

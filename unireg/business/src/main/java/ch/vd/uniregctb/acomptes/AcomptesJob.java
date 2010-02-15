package ch.vd.uniregctb.acomptes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.AcomptesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class AcomptesJob  extends JobDefinition {

	public static final String NAME = "AcomptesJob";
	private static final String CATEGORIE = "Stats";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String PERIODE_FISCALE = "PERIODE";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;


	private AcomptesService service;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	static {
		params = new ArrayList<JobParam>();
		{
			final JobParam param0 = new JobParam();
			param0.setDescription("Période fiscale");
			param0.setName(PERIODE_FISCALE);
			param0.setMandatory(false);
			param0.setType(new JobParamInteger());
			params.add(param0);
			final JobParam param1 = new JobParam();
			param1.setDescription("Nombre de threads");
			param1.setName(I_NB_THREADS);
			param1.setMandatory(false);
			param1.setType(new JobParamInteger());
			params.add(param1);
		}

		defaultParams = new HashMap<String, Object>();
		{
			RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
			defaultParams.put(I_NB_THREADS, Integer.valueOf(10));
		}
	}

	public AcomptesJob(int order, String description) {
		this(order, description, defaultParams);
	}

	public AcomptesJob(int order, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, order, description, params, defaultParams);
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
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère le nombre de threads paramétrés
		final int nbThreads;
		if (params.get(I_NB_THREADS) != null) {
			nbThreads = (Integer) params.get(I_NB_THREADS);
		}
		else {
			nbThreads = (Integer) defaultParams.get(I_NB_THREADS);
		}

		Integer annee = (Integer) params.get(PERIODE_FISCALE);

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final AcomptesResults results = service.produireAcomptes(dateTraitement, nbThreads, annee, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final AcomptesRapport rapport = (AcomptesRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des populations pour les bases acomptes en date du " + dateTraitement + " est terminée.", rapport);
	}

}

package ch.vd.uniregctb.listes.suisseoupermiscresident;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ListeContribuablesResidentsSansForVaudoisRapport;
import ch.vd.uniregctb.document.ListesNominativesRapport;
import ch.vd.uniregctb.listes.ListesTiersService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Job qui compile une liste des contribuables suisses ou avec permis C, dont l'adresse principale est sur Vaud
 * mais qui n'ont pas de for vaudois...
 */
public class ListeContribuablesResidentsSansForVaudoisJob extends JobDefinition {

	public static final String NAME = "ListeCtbsResidentsSansForVdJob";
	private static final String CATEGORIE = "Stats";

	public static final String I_NB_THREADS = "nbThreads";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	private ListesTiersService service;

	static {
		params = new ArrayList<JobParam>();
		{
			final JobParam param0 = new JobParam();
			param0.setDescription("Nombre de threads");
			param0.setName(I_NB_THREADS);
			param0.setMandatory(false);
			param0.setType(new JobParamInteger());
			params.add(param0);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(I_NB_THREADS, Integer.valueOf(10));
		}
	}

	public ListeContribuablesResidentsSansForVaudoisJob(int order, String description) {
		this(order, description, defaultParams);
	}

	public ListeContribuablesResidentsSansForVaudoisJob(int order, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, order, description, params, defaultParams);
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setService(ListesTiersService service) {
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

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final ListeContribuablesResidentsSansForVaudoisResults results = service.produireListeContribuablesSuissesOuPermisCResidentsMaisSansForVd(dateTraitement, nbThreads, statusManager);
		results.sort();
		results.end();

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListeContribuablesResidentsSansForVaudoisRapport rapport = (ListeContribuablesResidentsSansForVaudoisRapport) template.execute(new TransactionCallback() {
			public ListeContribuablesResidentsSansForVaudoisRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des listes des contribuables suisses ou permis C avec adresse principale sur le canton mais sans for vaudois en date du " + dateTraitement + " est terminée.", rapport);
	}
}

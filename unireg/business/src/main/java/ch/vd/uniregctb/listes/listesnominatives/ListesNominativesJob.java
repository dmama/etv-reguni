package ch.vd.uniregctb.listes.listesnominatives;

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
import ch.vd.uniregctb.document.ListesNominativesRapport;
import ch.vd.uniregctb.listes.ListesTiersService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Batch d'extraction des listes nominatives
 * -> TOUS les contribuables avec leurs numéro, nom et prénom
 */
public class ListesNominativesJob extends JobDefinition {

	public static final String NAME = "ListesNominativesJob";
	private static final String CATEGORIE = "Stats";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String E_ADRESSES_INCLUSES = "typeAdresses";
	public static final String B_CONTRIBUABLES = "avecContribuables";
	public static final String B_DEBITEURS = "avecDebiteurs";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	private ListesTiersService service;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	static {
		params = new ArrayList<JobParam>();
		{
			final JobParam param0 = new JobParam();
			param0.setDescription("Nombre de threads");
			param0.setName(I_NB_THREADS);
			param0.setMandatory(false);
			param0.setType(new JobParamInteger());
			params.add(param0);

			final JobParam param1 = new JobParam();
			param1.setDescription("Type d'adresses incluses");
			param1.setName(E_ADRESSES_INCLUSES);
			param1.setMandatory(false);
			param1.setType(new JobParamEnum(TypeAdresse.class));
			params.add(param1);

			final JobParam param2 = new JobParam();
			param2.setDescription("Inclure les personnes physiques / ménages");
			param2.setName(B_CONTRIBUABLES);
			param2.setMandatory(false);
			param2.setType(new JobParamBoolean());
			params.add(param2);

			final JobParam param3 = new JobParam();
			param3.setDescription("Inclure les débiteurs de prestations imposables");
			param3.setName(B_DEBITEURS);
			param3.setMandatory(false);
			param3.setType(new JobParamBoolean());
			params.add(param3);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(I_NB_THREADS, 2);
			defaultParams.put(E_ADRESSES_INCLUSES, TypeAdresse.AUCUNE);
			defaultParams.put(B_CONTRIBUABLES, Boolean.TRUE);
			defaultParams.put(B_DEBITEURS, Boolean.TRUE);
		}
	}

	public ListesNominativesJob(int order, String description) {
		this(order, description, defaultParams);
	}

	public ListesNominativesJob(int order, String description, HashMap<String, Object> defaultParams) {
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

	@SuppressWarnings({"unchecked"})
	private static <T> T getParametre(Class<T> clazz, String nom, HashMap<String, Object> params) {
		T valeur = (T) params.get(nom);
		if (valeur == null) {
			valeur = (T) defaultParams.get(nom);
		}
		return valeur;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère le nombre de threads paramétrés
		final int nbThreads = getParametre(Integer.class, I_NB_THREADS, params);

		// doit-on également mettre les adresses ?
		final TypeAdresse adressesIncluses = getParametre(TypeAdresse.class, E_ADRESSES_INCLUSES, params);

		// population à lister ?
		final boolean avecContribuables = getParametre(Boolean.class, B_CONTRIBUABLES, params);
		final boolean avecDebiteurs = getParametre(Boolean.class, B_DEBITEURS, params);

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final ListesNominativesResults results = service.produireListesNominatives(dateTraitement, nbThreads, adressesIncluses, avecContribuables, avecDebiteurs, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListesNominativesRapport rapport = (ListesNominativesRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des listes nominatives en date du " + dateTraitement + " est terminée.", rapport);
	}

}

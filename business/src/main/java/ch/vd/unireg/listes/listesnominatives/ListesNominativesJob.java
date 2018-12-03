package ch.vd.unireg.listes.listesnominatives;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ListesNominativesRapport;
import ch.vd.unireg.listes.ListesTiersService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamEnum;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamInteger;

/**
 * Batch d'extraction des listes nominatives -> TOUS les contribuables avec leurs numéro, nom et prénom
 */
public class ListesNominativesJob extends JobDefinition {

	public static final String NAME = "ListesNominativesJob";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String E_ADRESSES_INCLUSES = "typeAdresses";
	public static final String B_CONTRIBUABLES_PP = "avecContribuablesPP";
	public static final String B_CONTRIBUABLES_PM = "avecContribuablesPM";
	public static final String B_DEBITEURS = "avecDebiteurs";
	public static final String FILE_TIERS_LIST = "FILE_TIERS_LIST";

	private ListesTiersService service;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	public ListesNominativesJob(int order, String description) {
		super(NAME, JobCategory.STATS, order, description);

		final JobParam param0 = new JobParam();
		param0.setDescription("Nombre de threads");
		param0.setName(I_NB_THREADS);
		param0.setMandatory(true);
		param0.setType(new JobParamInteger());
		addParameterDefinition(param0, 2);

		final JobParam param1 = new JobParam();
		param1.setDescription("Type d'adresses incluses");
		param1.setName(E_ADRESSES_INCLUSES);
		param1.setMandatory(true);
		param1.setType(new JobParamEnum(TypeAdresse.class));
		addParameterDefinition(param1, TypeAdresse.AUCUNE);

		final JobParam param2 = new JobParam();
		param2.setDescription("Inclure les personnes physiques / ménages");
		param2.setName(B_CONTRIBUABLES_PP);
		param2.setMandatory(true);
		param2.setType(new JobParamBoolean());
		addParameterDefinition(param2, Boolean.TRUE);

		final JobParam param3 = new JobParam();
		param3.setDescription("Inclure les débiteurs de prestations imposables");
		param3.setName(B_DEBITEURS);
		param3.setMandatory(true);
		param3.setType(new JobParamBoolean());
		addParameterDefinition(param3, Boolean.TRUE);

		final JobParam param4 = new JobParam();
		param4.setDescription("Inclure les personnes morales");
		param4.setName(B_CONTRIBUABLES_PM);
		param4.setMandatory(true);
		param4.setType(new JobParamBoolean());
		addParameterDefinition(param4, Boolean.FALSE);

		{
			final JobParam param = new JobParam();
			param.setDescription("Ids des tiers (fichier CSV)");
			param.setName(FILE_TIERS_LIST);
			param.setMandatory(false);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
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
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère le nombre de threads paramétrés
		final int nbThreads = getStrictlyPositiveIntegerValue(params, I_NB_THREADS);

		// doit-on également mettre les adresses ?
		final TypeAdresse adressesIncluses = getEnumValue(params, E_ADRESSES_INCLUSES, TypeAdresse.class);

		// population à lister ?
		final boolean avecContribuablesPP = getBooleanValue(params, B_CONTRIBUABLES_PP);
		final boolean avecContribuablesPM = getBooleanValue(params, B_CONTRIBUABLES_PM);
		final boolean avecDebiteurs = getBooleanValue(params, B_DEBITEURS);

		final byte[] idsFile = getFileContent(params, FILE_TIERS_LIST);
		final Set<Long> ids = new HashSet<>(extractIdsFromCSV(idsFile));

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final ListesNominativesResults results = service.produireListesNominatives(nbThreads, adressesIncluses, avecContribuablesPP, avecContribuablesPM, dateTraitement, avecDebiteurs, statusManager, ids);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListesNominativesRapport rapport = template.execute(new TransactionCallback<ListesNominativesRapport>() {
			@Override
			public ListesNominativesRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des listes nominatives en date du " + dateTraitement + " est terminée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

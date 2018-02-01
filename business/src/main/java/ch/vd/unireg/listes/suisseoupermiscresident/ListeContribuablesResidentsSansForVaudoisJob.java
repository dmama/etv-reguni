package ch.vd.unireg.listes.suisseoupermiscresident;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ListeContribuablesResidentsSansForVaudoisRapport;
import ch.vd.unireg.listes.ListesTiersService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

/**
 * Job qui compile une liste des contribuables suisses ou avec permis C, dont l'adresse principale est sur Vaud
 * mais qui n'ont pas de for vaudois...
 */
public class ListeContribuablesResidentsSansForVaudoisJob extends JobDefinition {

	public static final String NAME = "ListeCtbsResidentsSansForVdJob";

	public static final String I_NB_THREADS = "nbThreads";

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	private ListesTiersService service;

	public ListeContribuablesResidentsSansForVaudoisJob(int order, String description) {
		super(NAME, JobCategory.STATS, order, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(I_NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 10);
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

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final ListeContribuablesResidentsSansForVaudoisResults results = service.produireListeContribuablesSuissesOuPermisCResidentsMaisSansForVd(dateTraitement, nbThreads, statusManager);
		results.sort();
		results.end();

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListeContribuablesResidentsSansForVaudoisRapport rapport = template.execute(new TransactionCallback<ListeContribuablesResidentsSansForVaudoisRapport>() {
			@Override
			public ListeContribuablesResidentsSansForVaudoisRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des listes des contribuables suisses ou permis C avec adresse principale sur le canton mais sans for vaudois en date du " + dateTraitement + " est terminée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

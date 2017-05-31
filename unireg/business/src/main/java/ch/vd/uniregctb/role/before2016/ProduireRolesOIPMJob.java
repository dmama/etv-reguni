package ch.vd.uniregctb.role.before2016;

import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RolesOIPMRapport;
import ch.vd.uniregctb.scheduler.JobCategory;

/**
 * Job qui produit les rôles pour l'OIPM (= l'ensemble des entreprises liées au canton)
 */
public class ProduireRolesOIPMJob extends AbstractProduireRolesJob {

	public static final String NAME = "ProduireRolesOIPMJob";

	public ProduireRolesOIPMJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		final RegDate today = RegDate.get();
		addParameterDefinition(createParamPeriodeFiscale(), today.year() - 1);
		addParameterDefinition(createParamNbThreads(), 4);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final int annee = getPeriodeFiscale(params);
		final int nbThreads = getNbThreads(params);
		final RegDate dateTraitement = getDateTraitement(params);

		final ProduireRolesOIPMResults results = getService().produireRolesPourOfficePersonnesMorales(annee, nbThreads, statusManager);

		// Produit le rapport dans une transaction read-write.
		final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
		template.setReadOnly(false);
		final RolesOIPMRapport rapport = template.execute(new TransactionCallback<RolesOIPMRapport>() {
			@Override
			public RolesOIPMRapport doInTransaction(TransactionStatus status) {
				return getRapportService().generateRapport(results, dateTraitement, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des rôles (OIPM) pour l'année " + annee + " est terminée.", rapport);
	}
}
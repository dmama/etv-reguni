package ch.vd.uniregctb.role.before2016;

import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RolesCommunesPMRapport;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamCommune;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Job qui produit les rôles PM pour les communes
 */
public class ProduireRolesPMCommunesJob extends AbstractProduireRolesJob {

	private static final String NAME = "ProduireRolesPMCommuneJob";
	private static final String NO_OFS_COMMUNE = "NO_OFS_COMMUNE";

	public ProduireRolesPMCommunesJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		final RegDate today = RegDate.get();
		addParameterDefinition(createParamPeriodeFiscale(), today.year() - 1);
		addParameterDefinition(createParamNbThreads(), 4);

		final JobParam param = new JobParam();
		param.setDescription("Nom d'une commune (optionnel)");
		param.setName(NO_OFS_COMMUNE);
		param.setMandatory(false);
		param.setType(new JobParamCommune(JobParamCommune.TypeCommune.COMMUNE_VD));
		addParameterDefinition(param, null);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final int annee = getPeriodeFiscale(params);
		final int nbThreads = getNbThreads(params);
		final Integer noOfsCommune = getOptionalIntegerValue(params, NO_OFS_COMMUNE);

		final ProduireRolesPMCommunesResults results;
		if (noOfsCommune != null) {
			results = getService().produireRolesPMPourUneCommune(annee, noOfsCommune, nbThreads, statusManager);
		}
		else {
			results = getService().produireRolesPMPourToutesCommunes(annee, nbThreads, statusManager);
		}

		// Produit le rapport dans une transaction read-write.
		final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
		template.setReadOnly(false);
		final RolesCommunesPMRapport rapport = template.execute(new TransactionCallback<RolesCommunesPMRapport>() {
			@Override
			public RolesCommunesPMRapport doInTransaction(TransactionStatus status) {
				return getRapportService().generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des rôles PM (communes) pour l'année " + annee + " est terminée.", rapport);
	}
}

package ch.vd.uniregctb.role;

import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RolesCommunesRapport;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamCommune;

/**
 * Job qui produit les rôles pour les communes
 */
public class ProduireRolesCommunesJob extends AbstractProduireRolesJob {

	public static final String NAME = "ProduireRolesCommuneJob";
	private static final String CATEGORIE = "Stats";

	public static final String NO_OFS_COMMUNE = "NO_OFS_COMMUNE";

	public ProduireRolesCommunesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final RegDate today = RegDate.get();
		addParameterDefinition(createParamPeriodeFiscale(), today.year() - 1);
		addParameterDefinition(createParamNbThreads(), 4);

		final JobParam param = new JobParam();
		param.setDescription("Nom d'une commune (optionnel)");
		param.setName(NO_OFS_COMMUNE);
		param.setMandatory(false);
		param.setType(new JobParamCommune());
		addParameterDefinition(param, null);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final int annee = getPeriodeFiscale(params);
		final int nbThreads = getNbThreads(params);
		final Integer noOfsCommune = getOptionalIntegerValue(params, NO_OFS_COMMUNE);

		final ProduireRolesCommunesResults results;
		if (noOfsCommune != null) {
			results = getService().produireRolesPourUneCommune(annee, noOfsCommune, nbThreads, statusManager);
		}
		else {
			results = getService().produireRolesPourToutesCommunes(annee, nbThreads, statusManager);
		}

		// Produit le rapport dans une transaction read-write.
		final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
		template.setReadOnly(false);
		final RolesCommunesRapport rapport = template.execute(new TransactionCallback<RolesCommunesRapport>() {
			public RolesCommunesRapport doInTransaction(TransactionStatus status) {
				return getRapportService().generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des rôles (communes) pour l'année " + annee + " est terminée.", rapport);
	}
}

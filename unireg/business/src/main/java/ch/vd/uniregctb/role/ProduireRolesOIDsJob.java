package ch.vd.uniregctb.role;

import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RolesOIDsRapport;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamOfficeImpot;

/**
 * Job qui produit les rôles pour les OID
 */
public class ProduireRolesOIDsJob extends AbstractProduireRolesJob {

	public static final String NAME = "ProduireRolesOIDJob";
	private static final String CATEGORIE = "Stats";

	public static final String NO_COL_OFFICE_IMPOT = "NO_COL_OFFICE_IMPOT";

	public ProduireRolesOIDsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final RegDate today = RegDate.get();
		addParameterDefinition(createParamPeriodeFiscale(), today.year() - 1);
		addParameterDefinition(createParamNbThreads(), 4);

		final JobParam param = new JobParam();
		param.setDescription("Nom d'un office d'impôt (optionnel)");
		param.setName(NO_COL_OFFICE_IMPOT);
		param.setMandatory(false);
		param.setType(new JobParamOfficeImpot());
		addParameterDefinition(param, null);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final int annee = getPeriodeFiscale(params);
		final int nbThreads = getNbThreads(params);
		final RegDate dateTraitement = getDateTraitement(params);
		final Integer noColOID = getOptionalIntegerValue(params, NO_COL_OFFICE_IMPOT);

		final ProduireRolesOIDsResults[] results;
		if (noColOID != null) {
			final ProduireRolesOIDsResults resultsUnOid = getService().produireRolesPourUnOfficeImpot(annee, noColOID, nbThreads, statusManager);
			results = new ProduireRolesOIDsResults[] { resultsUnOid };
		}
		else {
			results = getService().produireRolesPourTousOfficesImpot(annee, nbThreads, statusManager);
		}

		// Produit le rapport dans une transaction read-write.
		final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
		template.setReadOnly(false);
		final RolesOIDsRapport rapport = template.execute(new TransactionCallback<RolesOIDsRapport>() {
			public RolesOIDsRapport doInTransaction(TransactionStatus status) {
				return getRapportService().generateRapport(results, dateTraitement, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des rôles (OID) pour l'année " + annee + " est terminée.", rapport);
	}
}
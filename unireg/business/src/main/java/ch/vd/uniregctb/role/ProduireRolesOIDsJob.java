package ch.vd.uniregctb.role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			params.add(createParamPeriodeFiscale());
			params.add(createParamNbThreads());

			final JobParam param = new JobParam();
			param.setDescription("Nom d'un office d'impôt (optionnel)");
			param.setName(NO_COL_OFFICE_IMPOT);
			param.setMandatory(false);
			param.setType(new JobParamOfficeImpot());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			final RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
			defaultParams.put(NB_THREADS, 4);
		}
	}

	public ProduireRolesOIDsJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public ProduireRolesOIDsJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final int annee = getPeriodeFiscale(params);
		final int nbThreads = getNbThreads(params);
		final RegDate dateTraitement = getDateTraitement(params);

		final Integer noColOID = (Integer) params.get(NO_COL_OFFICE_IMPOT);

		final RolesOIDsRapport rapport;
		if (noColOID != null) {
			final ProduireRolesOIDsResults results = getService().produireRolesPourUnOfficeImpot(annee, noColOID, nbThreads, statusManager);

			// Produit le rapport dans une transaction read-write.
			final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
			template.setReadOnly(false);
			rapport = (RolesOIDsRapport) template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return getRapportService().generateRapport(new ProduireRolesOIDsResults[] { results }, dateTraitement, statusManager);
				}
			});
		}
		else {
			final ProduireRolesOIDsResults[] results = getService().produireRolesPourTousOfficesImpot(annee, nbThreads, statusManager);

			// Produit le rapport dans une transaction read-write.
			final TransactionTemplate template = new TransactionTemplate(getTransactionManager());
			template.setReadOnly(false);
			rapport = (RolesOIDsRapport) template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return getRapportService().generateRapport(results, dateTraitement, statusManager);
				}
			});
		}

		setLastRunReport(rapport);
		Audit.success("La production des rôles (OID) pour l'année " + annee + " est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}
}
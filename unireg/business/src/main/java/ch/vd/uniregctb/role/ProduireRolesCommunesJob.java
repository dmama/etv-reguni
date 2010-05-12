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

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			params.add(createParamPeriodeFiscale());
			params.add(createParamNbThreads());

			final JobParam param = new JobParam();
			param.setDescription("Nom d'une commune (optionnel)");
			param.setName(NO_OFS_COMMUNE);
			param.setMandatory(false);
			param.setType(new JobParamCommune());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
			defaultParams.put(NB_THREADS, 4);
		}
	}

	public ProduireRolesCommunesJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public ProduireRolesCommunesJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final int annee = getPeriodeFiscale(params);
		final int nbThreads = getNbThreads(params);

		final Integer noOfsCommune = (Integer) params.get(NO_OFS_COMMUNE);

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
		final RolesCommunesRapport rapport = (RolesCommunesRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return getRapportService().generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des rôles (communes) pour l'année " + annee + " est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

}

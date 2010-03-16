package ch.vd.uniregctb.role;

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
import ch.vd.uniregctb.document.RolesCommunesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamCommune;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamOfficeImpot;

/**
 * Job qui produit les rôles pour les communes
 */
public class ProduireRolesCommunesJob extends JobDefinition {

	private RoleService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	public static final String NAME = "ProduireRolesCommuneJob";
	private static final String CATEGORIE = "Stats";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NO_OFS_COMMUNE = "NO_OFS_COMMUNE";
	public static final String NO_COL_OFFICE_IMPOT = "NO_COL_OFFICE_IMPOT";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			params.add(param);

			JobParam param3 = new JobParam();
			param3.setDescription("Nom d'une commune (optionnel)");
			param3.setName(NO_OFS_COMMUNE);
			param3.setMandatory(false);
			param3.setType(new JobParamCommune());
			params.add(param3);

			JobParam param4 = new JobParam();
			param4.setDescription("Nom d'un office d'impôt (optionnel)");
			param4.setName(NO_COL_OFFICE_IMPOT);
			param4.setMandatory(false);
			param4.setType(new JobParamOfficeImpot());
			params.add(param4);
		}

		defaultParams = new HashMap<String, Object>();
		{
			RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
		}
	}

	public ProduireRolesCommunesJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public ProduireRolesCommunesJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	public void setService(RoleService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// Récupération des paramètres
		final Integer annee = (Integer) params.get(PERIODE_FISCALE);
		if (annee == null) {
			throw new RuntimeException("La période fiscale doit être spécifiée.");
		}

		final Integer noOfsCommune = (Integer) params.get(NO_OFS_COMMUNE);
		final Integer noColOID = (Integer) params.get(NO_COL_OFFICE_IMPOT);

		final ProduireRolesResults results;
		if (noOfsCommune != null) {
			results = service.produireRolesPourUneCommune(annee, noOfsCommune, statusManager);
		}
		else if (noColOID != null) {
			results = service.produireRolesPourUnOfficeImpot(annee, noColOID, statusManager);
		}
		else {
			results = service.produireRolesPourToutesCommunes(annee, statusManager);
		}

		// Produit le rapport dans une transaction read-write.
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final RolesCommunesRapport rapport = (RolesCommunesRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des rôles pour les communes pour l'année " + annee + " est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

}

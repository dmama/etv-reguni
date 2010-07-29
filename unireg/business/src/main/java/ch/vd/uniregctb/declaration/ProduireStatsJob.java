package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesDIs;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.document.StatistiquesCtbsRapport;
import ch.vd.uniregctb.document.StatistiquesDIsRapport;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job qui produit les statistiques des déclaration d'impôts ordinaires ou des contribuables
 */
public class ProduireStatsJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	private ParametreAppService paramsApp;

	public static final String NAME = "ProduireStatsJob";
	private static final String CATEGORIE = "Stats";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String STATS_TYPE = "TYPE";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	public enum Type {
		DECLARATIONS("déclarations"), CONTRIBUABLES("contribuables");

		private final String description;

		Type(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			params.add(param);
		}
		{
			JobParam param = new JobParam();
			param.setDescription("Type de statistiques");
			param.setName(STATS_TYPE);
			param.setMandatory(true);
			param.setType(new JobParamEnum(Type.class));
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
		}
	}

	public ProduireStatsJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	public void setService(DeclarationImpotService service) {
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

		// Récupération des paramètres
		final Integer annee = (Integer) params.get(PERIODE_FISCALE);
		if (annee == null) {
			throw new RuntimeException("La période fiscale doit être spécifiée.");
		}

		final Type type = (Type) params.get(STATS_TYPE);
		if (type == null) {
			throw new RuntimeException("Le type de statistiques doit être spécifié.");
		}

		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui

		if (annee >= dateTraitement.year()) {
			throw new RuntimeException("La période fiscale ne peut être >= à l'année en cours.");
		}
		if (annee < paramsApp.getPremierePeriodeFiscale()) {
			throw new RuntimeException("La période fiscale ne peut être < à l'année " + paramsApp.getPremierePeriodeFiscale());
		}

		final Document rapport;
		final StatusManager statusManager = getStatusManager();
		if (type == Type.DECLARATIONS) {

			// produit les informations de statistiques
			final StatistiquesDIs results = service.produireStatsDIs(annee, dateTraitement, statusManager);

			// Produit le rapport dans une transaction read-write
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(false);
			rapport = (StatistiquesDIsRapport) template.execute(new TransactionCallback() {
				public StatistiquesDIsRapport doInTransaction(TransactionStatus status) {
					return rapportService.generateRapport(results, statusManager);
				}
			});
		}
		else if (type == Type.CONTRIBUABLES) {

			// produit les informations de statistiques
			final StatistiquesCtbs results = service.produireStatsCtbs(annee, dateTraitement, statusManager);

			// Produit le rapport dans une transaction read-write
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(false);
			rapport = (StatistiquesCtbsRapport) template.execute(new TransactionCallback() {
				public StatistiquesCtbsRapport doInTransaction(TransactionStatus status) {
					return rapportService.generateRapport(results, statusManager);
				}
			});
		}
		else {
			throw new RuntimeException("Type de statistiques inconnu : " + type);
		}

		setLastRunReport(rapport);
		Audit.success("La production des statistiques pour l'année " + annee + " et sur les " + type.description() + " à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

	public void setParamsApp(ParametreAppService paramsApp) {
		this.paramsApp = paramsApp;
	}
}

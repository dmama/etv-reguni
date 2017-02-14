package ch.vd.uniregctb.declaration.ordinaire;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Job qui produit les statistiques des déclaration d'impôts ordinaires ou des contribuables
 */
public class ProduireStatsJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	private ParametreAppService paramsApp;

	public static final String NAME = "ProduireStatsJob";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String STATS_TYPE = "TYPE";
	public static final String POPULATION = "POPULATION";

	public enum Type {
		DECLARATIONS("déclarations"),
		CONTRIBUABLES("contribuables");

		private final String description;

		Type(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum Population {
		PP,
		PM
	}

	public ProduireStatsJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		{
			final RegDate today = RegDate.get();
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, today.year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Type de statistiques");
			param.setName(STATS_TYPE);
			param.setMandatory(true);
			param.setType(new JobParamEnum(Type.class));
			addParameterDefinition(param, Type.DECLARATIONS);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Population concernée");
			param.setName(POPULATION);
			param.setMandatory(true);
			param.setType(new JobParamEnum(Population.class));
			addParameterDefinition(param, Population.PP);
		}
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

	private static final Map<Population, ToIntFunction<ParametreAppService>> PREMIERE_PERIODE_SUPPLIERS = buildPremierePeriodeSuppliers();

	private static Map<Population, ToIntFunction<ParametreAppService>> buildPremierePeriodeSuppliers() {
		final Map<Population, ToIntFunction<ParametreAppService>> map = new EnumMap<>(Population.class);
		map.put(Population.PM, ParametreAppService::getPremierePeriodeFiscalePersonnesMorales);
		map.put(Population.PP, ParametreAppService::getPremierePeriodeFiscalePersonnesPhysiques);
		return map;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Récupération des paramètres
		final int annee = getIntegerValue(params, PERIODE_FISCALE);
		final Type type = getEnumValue(params, STATS_TYPE, Type.class);
		final Population population = getEnumValue(params, POPULATION, Population.class);
		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui

		if (annee >= dateTraitement.year()) {
			throw new RuntimeException("La période fiscale ne peut être postérieure ou égale à l'année en cours.");
		}

		final int premierePeriodeAutorisee = PREMIERE_PERIODE_SUPPLIERS.get(population).applyAsInt(paramsApp);
		if (annee < premierePeriodeAutorisee) {
			throw new RuntimeException("La période fiscale ne peut être antérieure à l'année " + premierePeriodeAutorisee + " pour une extraction de la population " + population);
		}

		final Document rapport;
		final StatusManager statusManager = getStatusManager();
		if (type == Type.DECLARATIONS) {

			// produit les informations de statistiques
			final StatistiquesDIs results;
			switch (population) {
			case PM:
				results = service.produireStatsDIsPM(annee, dateTraitement, statusManager);
				break;
			case PP:
				results = service.produireStatsDIsPP(annee, dateTraitement, statusManager);
				break;
			default:
				throw new RuntimeException("Type de population inconnu : " + population);
			}

			// Produit le rapport dans une transaction read-write
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(false);
			rapport = template.execute(status -> rapportService.generateRapport(results, statusManager));
		}
		else if (type == Type.CONTRIBUABLES) {

			// produit les informations de statistiques
			final StatistiquesCtbs results;
			switch (population) {
			case PM:
				results = service.produireStatsCtbsPM(annee, dateTraitement, statusManager);
				break;
			case PP:
				results = service.produireStatsCtbsPP(annee, dateTraitement, statusManager);
				break;
			default:
				throw new RuntimeException("Type de population inconnu : " + population);
			}

			// Produit le rapport dans une transaction read-write
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(false);
			rapport = template.execute(status -> rapportService.generateRapport(results, statusManager));
		}
		else {
			throw new RuntimeException("Type de statistiques inconnu : " + type);
		}

		setLastRunReport(rapport);
		Audit.success("La production des statistiques pour l'année " + annee + " et sur les " + type.description() + " à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	public void setParamsApp(ParametreAppService paramsApp) {
		this.paramsApp = paramsApp;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

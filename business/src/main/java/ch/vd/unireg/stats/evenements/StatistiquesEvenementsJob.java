package ch.vd.unireg.stats.evenements;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.document.StatistiquesEvenementsRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamInteger;

/**
 * Job qui génère des statistiques pour les événements reçus et traités par
 * unireg (civil, externe, identification contribuable...)
 */
public class StatistiquesEvenementsJob extends JobDefinition {

	public static final String NAME = "StatistiquesEvenementsJob";

	private static final String EVTS_CIVILS = "EVTS_CIVILS";
	private static final String EVTS_ENTREPRISE = "EVTS_ENTREPRISES";
	private static final String EVTS_EXTERNES = "EVTS_EXTERNES";
	private static final String EVTS_IDENT_CTB = "EVTS_IDENT_CTB";
	private static final String EVTS_NOTAIRES = "EVTS_NOTAIRES";
	private static final String DUREE_REFERENCE = "DUREE";

	private StatistiquesEvenementsService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	public StatistiquesEvenementsJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements civils (personnes)");
			param.setName(EVTS_CIVILS);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements civils (entreprises)");
			param.setName(EVTS_ENTREPRISE);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements externes");
			param.setName(EVTS_EXTERNES);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements d'identification");
			param.setName(EVTS_IDENT_CTB);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements notaires");
			param.setName(EVTS_NOTAIRES);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Durée de référence (jours)");
			param.setName(DUREE_REFERENCE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 7);
		}
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setService(StatistiquesEvenementsService service) {
		this.service = service;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// allons chercher les paramètres
		final boolean civils = getBooleanValue(params, EVTS_CIVILS);
		final boolean evenements = getBooleanValue(params, EVTS_ENTREPRISE);
		final boolean externes = getBooleanValue(params, EVTS_EXTERNES);
		final boolean identCtb = getBooleanValue(params, EVTS_IDENT_CTB);
		final boolean notaires = getBooleanValue(params, EVTS_NOTAIRES);
		final int dureeReference = getPositiveIntegerValue(params, DUREE_REFERENCE);
		final RegDate debutActivite = RegDate.get().addDays(- dureeReference);

		// lancement des extractions
		final StatsEvenementsCivilsPersonnesResults resultatsCivilsPersonnes = civils ? service.getStatistiquesEvenementsCivilsPersonnes(debutActivite) : null;
		final StatsEvenementsCivilsEntreprisesResults resultatsEntreprises = evenements ? service.getStatistiquesEvenementsCivilsEntreprises(debutActivite) : null;
		final StatsEvenementsExternesResults resultatsExternes = externes ? service.getStatistiquesEvenementsExternes() : null;
		final StatsEvenementsIdentificationContribuableResults resultatsIdentCtb = identCtb ? service.getStatistiquesEvenementsIdentificationContribuable(debutActivite) : null;
		final StatsEvenementsNotairesResults resultatsNotaires = notaires ? service.getStatistiquesEvenementsNotaires(debutActivite) : null;

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final StatistiquesEvenementsRapport rapport = template.execute(new TransactionCallback<StatistiquesEvenementsRapport>() {
			@Override
			public StatistiquesEvenementsRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(resultatsCivilsPersonnes, resultatsEntreprises, resultatsExternes, resultatsIdentCtb, resultatsNotaires, debutActivite, getStatusManager());
			}
		});

		setLastRunReport(rapport);
		audit.success("La production des statistiques des événements reçus en date du " + RegDate.get() + " est terminée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

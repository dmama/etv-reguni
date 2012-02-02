package ch.vd.uniregctb.stats.evenements;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.StatistiquesEvenementsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job qui génère des statistiques pour les événements reçus et traités par
 * unireg (civil, externe, identification contribuable...)
 */
public class StatistiquesEvenementsJob extends JobDefinition {

	private StatistiquesEvenementsService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	public static final String NAME = "StatistiquesEvenementsJob";
	private static final String CATEGORIE = "Stats";

	private static final String EVTS_CIVILS = "EVTS_CIVILS";
	private static final String EVTS_EXTERNES = "EVTS_EXTERNES";
	private static final String EVTS_IDENT_CTB = "EVTS_IDENT_CTB";
	private static final String DUREE_REFERENCE = "DUREE";

	public StatistiquesEvenementsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements civils");
			param.setName(EVTS_CIVILS);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements externes");
			param.setName(EVTS_EXTERNES);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Evénements d'identification");
			param.setName(EVTS_IDENT_CTB);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
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
		final boolean externes = getBooleanValue(params, EVTS_EXTERNES);
		final boolean identCtb = getBooleanValue(params, EVTS_IDENT_CTB);
		final int dureeReference = getStrictlyPositiveIntegerValue(params, DUREE_REFERENCE);
		final RegDate debutActivite = RegDate.get().addDays(- dureeReference);

		// lancement des extractions
		final StatsEvenementsCivilsRegPPResults resultatsCivilsRegPP;
		final StatsEvenementsCivilsEchResults resultatsCivilsEch;
		if (civils) {
			resultatsCivilsRegPP = service.getStatistiquesEvenementsCivilsRegPP();
			resultatsCivilsEch = service.getStatistiquesEvenementsCivilsEch(debutActivite);
		}
		else {
			resultatsCivilsRegPP = null;
			resultatsCivilsEch = null;
		}

		final StatsEvenementsExternesResults resultatsExternes;
		if (externes) {
			resultatsExternes = service.getStatistiquesEvenementsExternes();
		}
		else {
			resultatsExternes = null;
		}

		final StatsEvenementsIdentificationContribuableResults resultatsIdentCtb;
		if (identCtb) {
			resultatsIdentCtb = service.getStatistiquesEvenementsIdentificationContribuable(debutActivite);
		}
		else {
			resultatsIdentCtb = null;
		}

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final StatistiquesEvenementsRapport rapport = template.execute(new TransactionCallback<StatistiquesEvenementsRapport>() {
			@Override
			public StatistiquesEvenementsRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(resultatsCivilsRegPP, resultatsCivilsEch, resultatsExternes, resultatsIdentCtb, debutActivite, getStatusManager());
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des statistiques des événements reçus en date du " + RegDate.get() + " est terminée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

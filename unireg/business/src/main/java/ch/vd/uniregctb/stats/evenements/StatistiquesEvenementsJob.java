package ch.vd.uniregctb.stats.evenements;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.StatistiquesEvenementsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	private static final List<JobParam> params;
	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Evénements civils");
			param.setName(EVTS_CIVILS);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			params.add(param);
		}

		{
			JobParam param = new JobParam();
			param.setDescription("Evénements externes");
			param.setName(EVTS_EXTERNES);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			params.add(param);
		}

		{
			JobParam param = new JobParam();
			param.setDescription("Evénements d'identification");
			param.setName(EVTS_IDENT_CTB);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			params.add(param);
		}

		{
			JobParam param = new JobParam();
			param.setDescription("Durée de référence (jours)");
			param.setName(DUREE_REFERENCE);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(EVTS_CIVILS, Boolean.TRUE);
			defaultParams.put(EVTS_EXTERNES, Boolean.TRUE);
			defaultParams.put(EVTS_IDENT_CTB, Boolean.TRUE);
			defaultParams.put(DUREE_REFERENCE, 7);
		}
	}

	public StatistiquesEvenementsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
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
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		// allons chercher les paramètres
		final boolean civils = getBooleanValue(params, EVTS_CIVILS);
		final boolean externes = getBooleanValue(params, EVTS_EXTERNES);
		final boolean identCtb = getBooleanValue(params, EVTS_IDENT_CTB);
		Integer dureeReference = (Integer) params.get(DUREE_REFERENCE);
		if (dureeReference == null) {
			dureeReference = (Integer) defaultParams.get(DUREE_REFERENCE);
			Assert.notNull(dureeReference);
		}
		final RegDate debutActivite = RegDate.get().addDays(- dureeReference);

		// lancement des extractions
		final StatsEvenementsCivilsResults resultatsCivils;
		if (civils) {
			resultatsCivils = service.getStatistiquesEvenementsCivils(debutActivite);
		}
		else {
			resultatsCivils = null;
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
		final StatistiquesEvenementsRapport rapport = (StatistiquesEvenementsRapport) template.execute(new TransactionCallback() {
			public StatistiquesEvenementsRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(resultatsCivils, resultatsExternes, resultatsIdentCtb, debutActivite, getStatusManager());
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production des statistiques des événements reçus en date du " + RegDate.get() + " est terminée.", rapport);
	}
}

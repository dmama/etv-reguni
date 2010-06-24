package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.CorrectionFlagHabitantRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Job qui remet d'aplomb les flags "habitant" des personnes physiques en
 * fonction de leur for ouvert actuel
 */
public class CorrectionFlagHabitantJob extends JobDefinition {

	private static final String NAME = "CorrectionFlagHabitantJob";

	private static final String CATEGORIE = "Database";

	public static final String I_NB_THREADS = "nbThreads";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			final JobParam param0 = new JobParam();
			param0.setDescription("Nombre de threads");
			param0.setName(I_NB_THREADS);
			param0.setMandatory(false);
			param0.setType(new JobParamInteger());
			params.add(param0);
		}

		defaultParams = new HashMap<String, Object>();
		defaultParams.put(I_NB_THREADS, Integer.valueOf(10));
	}

	private TiersService tiersService;

	private RapportService rapportService;

	public CorrectionFlagHabitantJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public CorrectionFlagHabitantJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		// récupération du nombre de threads
		final int nbThreads;
		if (params.get(I_NB_THREADS) != null) {
			final int param = (Integer) params.get(I_NB_THREADS);
			if (param <= 0) {
				nbThreads = (Integer) defaultParams.get(I_NB_THREADS);
			}
			else {
				nbThreads = param;
			}
		}
		else {
			nbThreads = (Integer) defaultParams.get(I_NB_THREADS);
		}


		final StatusManager statusManager = getStatusManager();

		// correction des personnes physiques touchées
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults pp = tiersService.corrigeFlagHabitantSurPersonnesPhysiques(nbThreads, statusManager);

		// correction des ménages communs concernés
		final CorrectionFlagHabitantSurMenagesResults mc = tiersService.corrigeFlagHabitantSurMenagesCommuns(nbThreads, statusManager);

		// tri des données collectées
		pp.sort();
		mc.sort();

		// génération du rapport
		final CorrectionFlagHabitantRapport rapport = rapportService.generateRapport(pp, mc, statusManager);
		setLastRunReport(rapport);
		Audit.success("La correction des flags 'habitant' est terminée.", rapport);
	}

}

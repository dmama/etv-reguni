package ch.vd.unireg.tiers.rattrapage.flaghabitant;

import java.util.Map;

import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.CorrectionFlagHabitantRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.tiers.TiersService;

/**
 * Job qui remet d'aplomb les flags "habitant" des personnes physiques en
 * fonction de leur for ouvert actuel
 */
public class CorrectionFlagHabitantJob extends JobDefinition {

	private static final String NAME = "CorrectionFlagHabitantJob";

	public static final String I_NB_THREADS = "nbThreads";

	private TiersService tiersService;

	private RapportService rapportService;

	public CorrectionFlagHabitantJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(I_NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 10);
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// récupération du nombre de threads
		final int nbThreads = getStrictlyPositiveIntegerValue(params, I_NB_THREADS);

		final StatusManager statusManager = getStatusManager();

		// correction des personnes physiques touchées
		final CorrectionFlagHabitantResults res = tiersService.corrigeFlagHabitantSurPersonnesPhysiques(nbThreads, statusManager);

		// tri des données collectées
		res.sort();

		// génération du rapport
		final CorrectionFlagHabitantRapport rapport = rapportService.generateRapport(res, statusManager);
		setLastRunReport(rapport);
		Audit.success("La correction des flags 'habitant' est terminée.", rapport);
	}

}

package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.CorrectionFlagHabitantRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.TiersService;

import java.util.HashMap;

/**
 * Job qui remet d'aplomb les flags "habitant" des personnes physiques en
 * fonction de leur for ouvert actuel
 */
public class CorrectionFlagHabitantJob extends JobDefinition {

	private static final String NAME = "CorrectionFlagHabitantJob";

	private static final String CATEGORIE = "Database";

	private TiersService tiersService;

	private RapportService rapportService;

	public CorrectionFlagHabitantJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// correction des personnes physiques touchées
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults pp = tiersService.corrigeFlagHabitantSurPersonnesPhysiques(statusManager);

		// correction des ménages communs concernés
		final CorrectionFlagHabitantSurMenagesResults mc = tiersService.corrigeFlagHabitantSurMenagesCommuns(statusManager);

		// tri des données collectées
		pp.sort();
		mc.sort();

		// génération du rapport
		final CorrectionFlagHabitantRapport rapport = rapportService.generateRapport(pp, mc, statusManager);
		setLastRunReport(rapport);
		Audit.success("La correction des flags 'habitant' est terminée.", rapport);
	}

}

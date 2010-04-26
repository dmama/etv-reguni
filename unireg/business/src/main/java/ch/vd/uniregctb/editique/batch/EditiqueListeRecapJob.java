package ch.vd.uniregctb.editique.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.document.EnvoiLRsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class EditiqueListeRecapJob extends JobDefinition {

	public static final String NAME = "EditiqueListeRecapJob";
	private static final String CATEGORIE = "LR";

	public static final String S_PARAM_DATE_FIN_PERIODE = "DATE_FIN_PERIODE";

	private ListeRecapService listeRecapService;
	private RapportService rapportService;
	private static final HashMap<String, Object> defaultParams;

	private static final List<JobParam> params ;
	static {
		params = new ArrayList<JobParam>();
		JobParam param = new JobParam();
		param.setDescription("Date fin de période");
		param.setName(S_PARAM_DATE_FIN_PERIODE);
		param.setMandatory(true);
		param.setType(new JobParamRegDate());
		params.add(param);

		defaultParams = new HashMap<String, Object>();
		{
		}
	}

	public EditiqueListeRecapJob(int sortOrder) {
		this(sortOrder, defaultParams);
	}

	public EditiqueListeRecapJob(int sortOrder, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, "Créer, envoyer à l'éditique et imprimer les listes récapitulatives", params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		final RegDate dateFinPeriode = getRegDateValue(params, S_PARAM_DATE_FIN_PERIODE);
		final RegDate dateFinMoisPeriode = dateFinPeriode.getLastDayOfTheMonth();

		final StatusManager status = getStatusManager();

		final EnvoiLRsResults results = listeRecapService.imprimerAllLR(dateFinMoisPeriode, status);
		if (results == null) {
			final String message = String.format("L'envoi en masse des LRs pour le %s a échoué", RegDateHelper
					.dateToDisplayString(dateFinPeriode));
			Audit.error(message);
			return;
		}
		final EnvoiLRsRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		final String message = "L'envoi en masse des LRs pour le " + RegDateHelper.dateToDisplayString(dateFinPeriode) + " est terminé.";
		Audit.success(message, rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

	public void setListeRecapService(ListeRecapService listeRecapService) {
		this.listeRecapService = listeRecapService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

}

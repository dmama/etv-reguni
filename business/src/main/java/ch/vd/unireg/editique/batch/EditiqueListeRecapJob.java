package ch.vd.unireg.editique.batch;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.source.EnvoiLRsResults;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.document.EnvoiLRsRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class EditiqueListeRecapJob extends JobDefinition {

	public static final String NAME = "EditiqueListeRecapJob";
	public static final String S_PARAM_DATE_FIN_PERIODE = "DATE_FIN_PERIODE";

	private ListeRecapService listeRecapService;
	private RapportService rapportService;

	public EditiqueListeRecapJob(int sortOrder) {
		super(NAME, JobCategory.LR, sortOrder, "Créer, envoyer à l'éditique et imprimer les listes récapitulatives");

		final JobParam param = new JobParam();
		param.setDescription("Date fin de période");
		param.setName(S_PARAM_DATE_FIN_PERIODE);
		param.setMandatory(false);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// [SIFISC-15006] si la date n'est pas renseignée dans les paramètres du batch, on prend la fin du mois courant
		final RegDate dateFinPeriode = getRegDateValue(params, S_PARAM_DATE_FIN_PERIODE);
		final RegDate dateFinMoisPeriode = dateFinPeriode == null ? RegDate.get().getLastDayOfTheMonth() : dateFinPeriode.getLastDayOfTheMonth();

		final StatusManager status = getStatusManager();

		final EnvoiLRsResults results = listeRecapService.imprimerAllLR(dateFinMoisPeriode, status);
		if (results == null) {
			final String message = String.format("L'envoi en masse des LRs pour le %s a échoué", RegDateHelper.dateToDisplayString(dateFinMoisPeriode));
			audit.error(message);
			return;
		}
		final EnvoiLRsRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		final String message = "L'envoi en masse des LRs pour le " + RegDateHelper.dateToDisplayString(dateFinMoisPeriode) + " est terminé.";
		audit.success(message, rapport);
	}

	public void setListeRecapService(ListeRecapService listeRecapService) {
		this.listeRecapService = listeRecapService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}

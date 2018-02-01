package ch.vd.unireg.evenement.externe;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.TraiterEvenementExterneRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public class EvenementExterneHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementExterneHandlerJob";

	private EvenementExterneProcessor evenementExterneProcessor;
	private RapportService rapportService;
	public static final String NB_THREADS = "NB_THREADS";

	public EvenementExterneHandlerJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements externes
		status.setMessage("traitement des événements externes...");
		final TraiterEvenementExterneResult results = evenementExterneProcessor.traiteEvenementsExternes(dateTraitement, nbThreads, status);
		final TraiterEvenementExterneRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		Audit.success("La relance du traitement des événements externes à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementExterneProcessor(EvenementExterneProcessor evenementExterneProcessor) {
		this.evenementExterneProcessor = evenementExterneProcessor;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
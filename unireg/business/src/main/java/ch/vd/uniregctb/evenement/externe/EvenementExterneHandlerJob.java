package ch.vd.uniregctb.evenement.externe;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.TraiterEvenementExterneRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class EvenementExterneHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementExterneHandlerJob";
	private static final String CATEGORIE = "Events";

	private EvenementExterneProcessor evenementExterneProcessor;
	private RapportService rapportService;
	public static final String NB_THREADS = "NB_THREADS";

	public EvenementExterneHandlerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

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
		final TraiterEvenementExterneResult results = evenementExterneProcessor.traiteEvenementExterne(dateTraitement, nbThreads, status);
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
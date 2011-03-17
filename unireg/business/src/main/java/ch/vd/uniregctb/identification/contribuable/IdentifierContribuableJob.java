package ch.vd.uniregctb.identification.contribuable;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.IdentifierContribuableRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class IdentifierContribuableJob extends JobDefinition {

	private IdentificationContribuableService identificationService;
	private RapportService rapportService;


	public static final String NAME = "IdentifierContribuableJob";
	private static final String CATEGORIE = "Events";

	public static final String NB_THREADS = "NB_THREADS";

	public IdentifierContribuableJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	public IdentificationContribuableService getIdentificationService() {
		return identificationService;
	}

	public void setIdentificationService(IdentificationContribuableService identificationService) {
		this.identificationService = identificationService;
	}

	public RapportService getRapportService() {
		return rapportService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job dans une transaction.
		final StatusManager status = getStatusManager();
		final IdentifierContribuableResults results = identificationService.relancerIdentificationAutomatique(dateTraitement, nbThreads, status);
		final IdentifierContribuableRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La relance de l'identification des contribuables à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

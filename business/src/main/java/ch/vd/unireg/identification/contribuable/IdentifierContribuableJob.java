package ch.vd.unireg.identification.contribuable;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.IdentifierContribuableRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamLong;

public class IdentifierContribuableJob extends JobDefinition {

	public static final String NAME = "IdentifierContribuableJob";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String ID_MESSAGE = "ID_MESSAGE";

	private IdentificationContribuableService identificationService;
	private RapportService rapportService;

	public IdentifierContribuableJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);

		final JobParam paramIdMessage = new JobParam();
		paramIdMessage.setDescription("Id du message a traiter");
		paramIdMessage.setName(ID_MESSAGE);
		paramIdMessage.setMandatory(false);
		paramIdMessage.setType(new JobParamLong());
		addParameterDefinition(paramIdMessage, null);
	}

	public void setIdentificationService(IdentificationContribuableService identificationService) {
		this.identificationService = identificationService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);
		final Long idMessage = getLongValue(params,ID_MESSAGE);

		// Exécution du job dans une transaction.
		final StatusManager status = getStatusManager();
		final IdentifierContribuableResults results = identificationService.relancerIdentificationAutomatique(dateTraitement, nbThreads, status, idMessage);
		final IdentifierContribuableRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		audit.success("La relance de l'identification des contribuables à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

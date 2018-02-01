package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.DeterminationDIsPPRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job qui détermine les déclaration d'impôts à envoyer et crée des tâches en instance.
 */
public class DeterminerDIsPPJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;

	public static final String NAME = "DetermineDIsEnMasseJob";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_THREADS = "NB_THREADS";

	public DeterminerDIsPPJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PP, sortOrder, description);

		final RegDate today = RegDate.get();
		final JobParam param0 = new JobParam();
		param0.setDescription("Période fiscale");
		param0.setName(PERIODE_FISCALE);
		param0.setMandatory(true);
		param0.setType(new JobParamInteger());
		addParameterDefinition(param0, today.year() - 1);

		final JobParam param1 = new JobParam();
		param1.setDescription("Nombre de threads");
		param1.setName(NB_THREADS);
		param1.setMandatory(true);
		param1.setType(new JobParamInteger());
		addParameterDefinition(param1, 4);

		final JobParam param2 = new JobParam();
		param2.setDescription("Date de traitement");
		param2.setName(DATE_TRAITEMENT);
		param2.setMandatory(false);
		param2.setType(new JobParamRegDate());
		addParameterDefinition(param2, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Récupération des paramètres
		final int annee = getIntegerValue(params, PERIODE_FISCALE);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job dans une transaction.
		final StatusManager status = getStatusManager();
		final DeterminationDIsPPResults results = service.determineDIsPPAEmettre(annee, dateTraitement, nbThreads, status);
		final DeterminationDIsPPRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La détermination des DIs PP à envoyer pour l'année " + annee + " à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.DeterminationDIsPMRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class DeterminerDIsPMJob extends JobDefinition {

	public static final String NAME = "DetermineDIsPMEnMasseJob";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_THREADS = "NB_THREADS";

	private DeclarationImpotService service;
	private RapportService rapportService;

	public DeterminerDIsPMJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PM, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());

			// sur le premier trimestre, la valeur par défaut de la PF est l'année précédente... après, c'est l'année courante
			final RegDate today = RegDate.get();
			addParameterDefinition(param, today.month() < 4 ? today.year() - 1 : today.year());
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
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
		final DeterminationDIsPMResults results = service.determineDIsPMAEmettre(annee, dateTraitement, nbThreads, status);
		final DeterminationDIsPMRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La détermination des DIs PM à envoyer pour l'année " + annee + " à la date du "
				              + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

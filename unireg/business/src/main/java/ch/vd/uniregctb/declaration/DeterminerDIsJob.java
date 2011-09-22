package ch.vd.uniregctb.declaration;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsResults;
import ch.vd.uniregctb.document.DeterminationDIsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui détermine les déclaration d'impôts à envoyer et crée des tâches en instance.
 */
public class DeterminerDIsJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;

	public static final String NAME = "DetermineDIsEnMasseJob";
	private static final String CATEGORIE = "DI";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_THREADS = "NB_THREADS";

	public DeterminerDIsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

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
		final DeterminationDIsResults results = service.determineDIsAEmettre(annee, dateTraitement, nbThreads, status);
		final DeterminationDIsRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La détermination des DIs à envoyer pour l'année " + annee + " à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}

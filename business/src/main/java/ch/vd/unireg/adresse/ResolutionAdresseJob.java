package ch.vd.unireg.adresse;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ResolutionAdresseRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public class ResolutionAdresseJob extends JobDefinition{

	public static final String NAME = "ResolutionAdresseJob";
	public static final String NB_THREADS = "NB_THREADS";

	private AdresseService adresseService;
	private RapportService rapportService;

	public ResolutionAdresseJob(int sortOrder, String description) {
		super(NAME, JobCategory.TIERS, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job dans une transaction.
		final StatusManager status = getStatusManager();
		final ResolutionAdresseResults results = adresseService.resoudreAdresse(dateTraitement, nbThreads, status);
		final ResolutionAdresseRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		audit.success("La résolution des adresses à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}
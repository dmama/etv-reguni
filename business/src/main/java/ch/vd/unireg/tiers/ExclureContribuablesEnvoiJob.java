package ch.vd.unireg.tiers;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ExclureContribuablesEnvoiRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job qui applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros, et génère un rapport.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ExclureContribuablesEnvoiJob extends JobDefinition {

	public static final String NAME = "ExclureContribuablesEnvoiJob";
	public static final String LISTE_CTBS = "LISTE_CTBS";
	public static final String DATE_LIMITE = "DATE_LIMITE";

	private TiersService tiersService;
	private RapportService rapportService;

	public ExclureContribuablesEnvoiJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PP, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV des numéros de contribuables à exclure");
			param.setName(LISTE_CTBS);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date limite d'exclusion");
			param.setName(DATE_LIMITE);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateLimite = getRegDateValue(params, DATE_LIMITE);
		final byte[] listeCtbsCsv = getFileContent(params, LISTE_CTBS);

		final List<Long> ids = extractIdsFromCSV(listeCtbsCsv);
		final StatusManager status = getStatusManager();

		final ExclureContribuablesEnvoiResults results = tiersService.setDateLimiteExclusion(ids, dateLimite, status);
		final ExclureContribuablesEnvoiRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		audit.success("L'exclusion des contribuables de l'envoi automatique est terminée.", rapport);
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

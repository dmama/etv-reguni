package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ExclureContribuablesEnvoiRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros, et génère un rapport.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ExclureContribuablesEnvoiJob extends JobDefinition {

	public static final String NAME = "ExclureContribuablesEnvoiJob";
	private static final String CATEGORIE = "DI";

	public static final String LISTE_CTBS = "LISTE_CTBS";
	public static final String DATE_LIMITE = "DATE_LIMITE";

	private static final List<JobParam> params;

	private TiersService tiersService;
	private RapportService rapportService;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Fichier CSV des numéros de contribuables à exclure");
			param.setName(LISTE_CTBS);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			params.add(param);
		}
		{
			JobParam param = new JobParam();
			param.setDescription("Date limite d'exclusion");
			param.setName(DATE_LIMITE);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			params.add(param);
		}
	}

	public ExclureContribuablesEnvoiJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final RegDate dateLimite = getRegDateValue(params, DATE_LIMITE);
		if (dateLimite == null) {
			throw new RuntimeException("La date limite doit être spécifiée.");
		}

		final byte[] listeCtbsCsv = (byte[]) params.get(LISTE_CTBS);
		if (listeCtbsCsv == null) {
			throw new RuntimeException("La liste des contribuables doit être spécifiée.");
		}

		final List<Long> ids = extractIdsFromCSV(listeCtbsCsv);
		final StatusManager status = getStatusManager();

		final ExclureContribuablesEnvoiResults results = tiersService.setDateLimiteExclusion(ids, dateLimite, status);
		final ExclureContribuablesEnvoiRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("L'exclusion des contribuables de l'envoi automatique est terminée.", rapport);
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}

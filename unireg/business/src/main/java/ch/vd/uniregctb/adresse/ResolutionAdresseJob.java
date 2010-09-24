package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.IdentifierContribuableRapport;
import ch.vd.uniregctb.document.ResolutionAdresseRapport;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class ResolutionAdresseJob extends JobDefinition{

	private AdresseService adresseService;
	private RapportService rapportService;


	public static final String NAME = "ResolutionAdresseJob";
	private static final String CATEGORIE = "Tiers";


	public static final String NB_THREADS = "NB_THREADS";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;
	static {
		params = new ArrayList<JobParam>();
		{

			JobParam param1 = new JobParam();
			param1.setDescription("Nombre de threads");
			param1.setName(NB_THREADS);
			param1.setMandatory(true);
			param1.setType(new JobParamInteger());
			params.add(param1);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(NB_THREADS, 4);

		}
	}



	public ResolutionAdresseJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public ResolutionAdresseJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}




	public RapportService getRapportService() {
		return rapportService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final int nbThreads = getIntegerValue(params, NB_THREADS);
		if (nbThreads <= 0) {
			throw new RuntimeException("Le nombre de threads doit être un entier positif.");
		}

		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job dans une transaction.
		final StatusManager status = getStatusManager();
		final ResolutionAdresseResults results = adresseService.ResoudreAdresse(dateTraitement, nbThreads, status);
		final ResolutionAdresseRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La résolution des adresses à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}
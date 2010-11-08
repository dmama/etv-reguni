package ch.vd.uniregctb.metier;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ComparerSituationFamilleRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.situationfamille.ComparerSituationFamilleResults;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;

public class ComparerForFiscalEtCommuneJob extends JobDefinition{


	private MetierService metierService;
	private RapportService rapportService;

	public static final String NAME = "ComparerForFiscalEtCommuneJob";
	private static final String CATEGORIE = "Fors";

	public static final String NB_THREADS = "NB_THREADS";


	public ComparerForFiscalEtCommuneJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();	
	}


	public MetierService getMetierService() {
		return metierService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
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
		  //TODO(XSIBNM) implementer la methode de comparaison dans le métierServic

		setLastRunReport(null);
		Audit.success("La comparaison des situations de famille à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", null);
	}
}

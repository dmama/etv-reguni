package ch.vd.uniregctb.situationfamille;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ReinitialiserBaremeDoubleGainRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui réinitialise à la valeur NORMAL les barèmes double-gains des ménages-communs sourciers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ReinitialiserBaremeDoubleGainJob extends JobDefinition {

	public static final String NAME = "ReinitialiserBaremeDoubleGainJob";
	private static final String CATEGORIE = "LR";

	private SituationFamilleService service;
	private RapportService rapportService;

	private static final List<JobParam> params;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			params.add(param);
		}
	}

	public ReinitialiserBaremeDoubleGainJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		params.get(0).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();

		final ReinitialiserBaremeDoubleGainResults results = service.reinitialiserBaremeDoubleGain(dateTraitement, statusManager);
		final ReinitialiserBaremeDoubleGainRapport rapport = rapportService.generateRapport(results, statusManager);

		setLastRunReport(rapport);
		Audit.success("La réinitialisation des barèmes double-gain est terminée.", rapport);
	}

	public void setSituationFamilleService(SituationFamilleService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}

package ch.vd.unireg.situationfamille;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ReinitialiserBaremeDoubleGainRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job qui réinitialise à la valeur NORMAL les barèmes double-gains des ménages-communs sourciers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ReinitialiserBaremeDoubleGainJob extends JobDefinition {

	public static final String NAME = "ReinitialiserBaremeDoubleGainJob";

	private SituationFamilleService service;
	private RapportService rapportService;
	private AuditManager audit;

	public ReinitialiserBaremeDoubleGainJob(int sortOrder, String description) {
		super(NAME, JobCategory.LR, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Date de traitement");
		param.setName(DATE_TRAITEMENT);
		param.setMandatory(false);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();

		final ReinitialiserBaremeDoubleGainResults results = service.reinitialiserBaremeDoubleGain(dateTraitement, statusManager);
		final ReinitialiserBaremeDoubleGainRapport rapport = rapportService.generateRapport(results, statusManager);

		setLastRunReport(rapport);
		audit.success("La réinitialisation des barèmes double-gain est terminée.", rapport);
	}

	public void setSituationFamilleService(SituationFamilleService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setService(SituationFamilleService service) {
		this.service = service;
	}
}

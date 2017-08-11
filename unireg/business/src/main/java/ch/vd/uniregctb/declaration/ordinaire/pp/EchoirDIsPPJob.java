package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.document.EchoirDIsPPRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui fait passer à l'état <i>ECHUE</i> les déclarations sommées et dont le délai de retour est dépassé.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EchoirDIsPPJob extends JobDefinition {

	private DeclarationImpotService diService;
	private RapportService rapportService;

	public static final String NAME = "EchoirDIsJob";

	public EchoirDIsPPJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PP, sortOrder, description);

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

	public void setDeclarationImpotService(DeclarationImpotService service) {
		this.diService = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager status = getStatusManager();

		final EchoirDIsPPResults results = diService.echoirDIsPPHorsDelai(dateTraitement, status);
		final EchoirDIsPPRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("Le passage à l'état 'échu' des DIs PP sommées est terminé.", rapport);
	}

}

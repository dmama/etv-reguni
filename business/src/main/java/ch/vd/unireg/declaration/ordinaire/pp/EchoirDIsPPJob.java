package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.EchoirDIsPPRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job qui fait passer à l'état <i>ECHU</i> les déclarations sommées et dont le délai de retour est dépassé.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EchoirDIsPPJob extends JobDefinition {

	public static final String NAME = "EchoirDIsJob";

	private DeclarationImpotService diService;
	private RapportService rapportService;

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
		audit.success("Le passage à l'état 'échu' des DIs PP sommées est terminé.", rapport);
	}

}

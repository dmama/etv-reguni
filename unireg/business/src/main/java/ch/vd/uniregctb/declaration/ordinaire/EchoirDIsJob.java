package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.EchoirDIsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui fait passer à l'état <i>ECHUE</i> les déclarations sommées et dont le délai de retour est dépassé.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EchoirDIsJob extends JobDefinition {

	private DeclarationImpotService diService;
	private RapportService rapportService;

	public static final String NAME = "EchoirDIsJob";
	private static final String CATEGORIE = "DI";

	public EchoirDIsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Date de traitement");
		param.setName(DATE_TRAITEMENT);
		param.setMandatory(false);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
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

		final EchoirDIsResults results = diService.echoirDIsHorsDelai(dateTraitement, status);
		final EchoirDIsRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("Le passage à l'état 'échu' des DIs sommées est terminée.", rapport);
	}

}

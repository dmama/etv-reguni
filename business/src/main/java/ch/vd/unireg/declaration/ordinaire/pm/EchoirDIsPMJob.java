package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.EchoirDIsPMRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class EchoirDIsPMJob extends JobDefinition {

	private static final String NAME = "EchoirDIsPMJob";

	private DeclarationImpotService declarationImpotService;
	private RapportService rapportService;

	public EchoirDIsPMJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PM, sortOrder, description);

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

	public void setDeclarationImpotService(DeclarationImpotService declarationImpotService) {
		this.declarationImpotService = declarationImpotService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitement = getDateTraitement(params);
		final EchoirDIsPMResults results = declarationImpotService.echoirDIsPMHorsDelai(dateTraitement, getStatusManager());
		final EchoirDIsPMRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		audit.success("Le passage à l'état 'échu' des DIs PM sommées est terminé.", rapport);
	}
}

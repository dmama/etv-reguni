package ch.vd.uniregctb.declaration.ordinaire.common;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.document.DemandeDelaiCollectiveRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Batch qui déclenche le traitement d'une demande de délai collective.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DemandeDelaiCollectiveJob extends JobDefinition {

	private DeclarationImpotService diService;
	private RapportService rapportService;

	public static final String NAME = "DemandeDelaiCollectiveJob";

	public static final String FICHIER = "FICHIER";
	public static final String DELAI = "DELAI";
	public static final String PERIODE = "PERIODE";

	public DemandeDelaiCollectiveJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier de demande");
			param.setName(FICHIER);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Délai accordé");
			param.setName(DELAI);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	public void setService(DeclarationImpotService service) {
		this.diService = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate delai = getRegDateValue(params, DELAI);
		if (delai.isBeforeOrEqual(RegDate.get())) {
			throw new RuntimeException("Le délai doit être après la date du jour.");
		}

		final RegDate dateTraitement = getDateTraitement(params);
		final List<Long> ids = extractIdsFromCSV(getFileContent(params, FICHIER));
		final int pf = getIntegerValue(params, PERIODE);

		final StatusManager status = getStatusManager();
		final DemandeDelaiCollectiveResults results = diService.traiterDemandeDelaiCollective(ids, pf, delai, dateTraitement, status);
		final DemandeDelaiCollectiveRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La demande de délai collective a été traitée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

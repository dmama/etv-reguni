package ch.vd.unireg.declaration.ordinaire.common;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.DemandeDelaiCollectiveRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Batch qui déclenche le traitement d'une demande de délai collective.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DemandeDelaiCollectiveJob extends JobDefinition {

	public static final String NAME = "DemandeDelaiCollectiveJob";
	public static final String FICHIER = "FICHIER";
	public static final String DELAI = "DELAI";
	public static final String PERIODE = "PERIODE";

	private DeclarationImpotService diService;
	private RapportService rapportService;

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

	public void setAudit(AuditManager audit) {
		this.audit = audit;
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
		audit.success("La demande de délai collective a été traitée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

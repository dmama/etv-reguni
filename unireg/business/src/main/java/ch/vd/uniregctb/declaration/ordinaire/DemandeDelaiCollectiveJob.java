package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DemandeDelaiCollectiveRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
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
	private static final String CATEGORIE = "DI";

	public static final String FICHIER = "FICHIER";
	public static final String DELAI = "DELAI";

	public DemandeDelaiCollectiveJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Fichier de demande");
		param.setName(FICHIER);
		param.setMandatory(true);
		param.setType(new JobParamFile());
		addParameterDefinition(param, null);

		final JobParam param2 = new JobParam();
		param2.setDescription("Délai");
		param2.setName(DELAI);
		param2.setMandatory(true);
		param2.setType(new JobParamRegDate());
		addParameterDefinition(param2, null);

		final JobParam param3 = new JobParam();
		param3.setDescription("Date de traitement");
		param3.setName(DATE_TRAITEMENT);
		param3.setMandatory(false);
		param3.setType(new JobParamRegDate());
		addParameterDefinition(param3, null);
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

		final byte[] idsFile = getFileContent(params, FICHIER);
		final RegDate delai = getRegDateValue(params, DELAI);
		if (delai.isBeforeOrEqual(RegDate.get())) {
			throw new RuntimeException("Le délai doit être après la date du jour.");
		}

		final RegDate dateTraitement = getDateTraitement(params);
		final List<Long> ids = extractIdsFromCSV(idsFile);
		final int annee = delai.year() - 1; // la période fiscale est déduite du délai, un peu étrange mais c'est comme ça...

		final StatusManager status = getStatusManager();
		final DemandeDelaiCollectiveResults results = diService.traiterDemandeDelaiCollective(ids, annee, delai, dateTraitement, status);
		final DemandeDelaiCollectiveRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La demande de délai collective a été traitée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

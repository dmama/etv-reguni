package ch.vd.uniregctb.editique.batch;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionChemisesTOResults;
import ch.vd.uniregctb.document.ImpressionChemisesTORapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamOfficeImpot;

/**
 * Job qui prends toutes les DI échues qui n'ont pas encore fait l'objet d'une impression de chemise TO et qui envoie cette chemise à
 * l'éditique
 */
public class ImpressionChemisesTOJob extends JobDefinition {

	public static final String NAME = "ImpressionChemisesTOJob";
	private static final String CATEGORIE = "DI";

	public static final String PARAM_NB_MAX = "NB_MAX";
	public static final String NO_COL_OFFICE_IMPOT = "NO_COL_OFFICE_IMPOT";

	private DeclarationImpotService declarationImpotService;
	private RapportService rapportService;

	public ImpressionChemisesTOJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre maximal de chemises TO imprimées (0 = pas de limite)");
		param.setName(PARAM_NB_MAX);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 10000);

		final JobParam param2 = new JobParam();
		param2.setDescription("Office d'impôt (optionnel)");
		param2.setName(NO_COL_OFFICE_IMPOT);
		param2.setMandatory(false);
		param2.setType(new JobParamOfficeImpot());
		addParameterDefinition(param2, null);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nombreMax = getIntegerValue(params, PARAM_NB_MAX);
		final Integer noColOid = getOptionalIntegerValue(params, NO_COL_OFFICE_IMPOT);
		final ImpressionChemisesTOResults results = declarationImpotService.envoiChemisesTaxationOffice(nombreMax, noColOid, getStatusManager());
		if (results == null) {
			Audit.error("L'envoi en masse des impressions des chemises TO a échoué.");
			return;
		}

		final ImpressionChemisesTORapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("L'envoi en masse des impressions des chemises TO est terminé.", rapport);
	}

	public void setDeclarationImpotService(DeclarationImpotService declarationImpotService) {
		this.declarationImpotService = declarationImpotService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}

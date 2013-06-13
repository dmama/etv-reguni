package ch.vd.uniregctb.editique.batch;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.document.EnvoiSommationLRsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamRegDate;
import ch.vd.uniregctb.type.CategorieImpotSource;

public class EditiqueSommationLRJob extends JobDefinition {

	public static final String NAME = "EditiqueSommationLRJob";
	private static final String CATEGORIE = "LR";
	private static final String FIN_PERIODE = "FIN_PERIODE";

	public static final String CATEGORIE_DEB = "CATEGORIE_DEB";

	private ListeRecapService listeRecapService;

	private RapportService rapportService;

	public EditiqueSommationLRJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Imprimer les sommations des listes recapitulatives");

		final JobParam param0 = new JobParam();
		param0.setDescription("Catégorie de débiteurs");
		param0.setName(CATEGORIE_DEB);
		param0.setMandatory(false);
		param0.setType(new JobParamEnum(CategorieImpotSource.class));
		addParameterDefinition(param0, null);

		final JobParam param1 = new JobParam();
		param1.setDescription("Fin de période");
		param1.setName(FIN_PERIODE);
		param1.setMandatory(false);
		param1.setType(new JobParamRegDate());
		addParameterDefinition(param1, null);

		final JobParam param2 = new JobParam();
		param2.setDescription("Date de traitement");
		param2.setName(DATE_TRAITEMENT);
		param2.setMandatory(false);
		param2.setType(new JobParamRegDate());
		addParameterDefinition(param2, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Récupération de la date de traitement
		final RegDate date = getRegDateValue(params, FIN_PERIODE); // [UNIREG-2109]
		final RegDate dateFinPeriode = (date != null ? endOfMonth(date) : null);
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager status = getStatusManager();
		final CategorieImpotSource categorie = getEnumValue(params, CATEGORIE_DEB, CategorieImpotSource.class);

		// Sommation des LRs
		final EnvoiSommationLRsResults results = listeRecapService.sommerAllLR(categorie, dateFinPeriode, dateTraitement, status);
		if (results == null) {
			Audit.error("L'envoi en masse des sommations de LRs  a échoué");
			return;
		}

		// Génération du rapport
		final EnvoiSommationLRsRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		Audit.success("L'envoi en masse des sommations LRs est terminé.", rapport);
	}

	private RegDate endOfMonth(RegDate date) {
		return RegDate.get(date.year(), date.month(), 1).addMonths(1).addDays(-1);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setListeRecapService(ListeRecapService listeRecapService) {
		this.listeRecapService = listeRecapService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

}

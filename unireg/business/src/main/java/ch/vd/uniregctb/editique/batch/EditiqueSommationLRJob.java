package ch.vd.uniregctb.editique.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	public static final String CATEGORIE_DEB = "CATEGORIE_DEB";

	private ListeRecapService listeRecapService;

	private RapportService rapportService;

	private static List<JobParam> params ;

	static {
		params = new ArrayList<JobParam>() ;

		JobParam param0 = new JobParam();
		param0.setDescription("Catégorie de débiteurs");
		param0.setName(CATEGORIE_DEB);
		param0.setMandatory(false);
		param0.setType(new JobParamEnum(CategorieImpotSource.class));
		params.add(param0);

		JobParam param1 = new JobParam();
		param1.setDescription("Date de traitement");
		param1.setName(DATE_TRAITEMENT);
		param1.setMandatory(false);
		param1.setType(new JobParamRegDate());
		params.add(param1);

	}

	public EditiqueSommationLRJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Imprimer les sommations des listes recapitulatives", params);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		// Récupération de la date de traitement
		final RegDate date = getRegDateValue(params, DATE_TRAITEMENT); // [UNIREG-2003] la date de traitement est toujours affichée
		final RegDate dateTraitement = (date != null ? date : RegDate.get());
		final StatusManager status = getStatusManager();

		final CategorieImpotSource categorie = (CategorieImpotSource) params.get(CATEGORIE_DEB);

		// Sommation des LRs
		final EnvoiSommationLRsResults results = listeRecapService.sommerAllLR(categorie, dateTraitement, status);
		if (results == null) {
			Audit.error( String.format("L'envoi en masse des sommations de LRs  a échoué"));
			return;
		}

		// Génération du rapport
		final EnvoiSommationLRsRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		Audit.success("L'envoi en masse des sommations LRs est terminé.", rapport);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setListeRecapService(ListeRecapService listeRecapService) {
		this.listeRecapService = listeRecapService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

}

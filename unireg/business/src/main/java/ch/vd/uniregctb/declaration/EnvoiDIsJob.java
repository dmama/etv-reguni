package ch.vd.uniregctb.declaration;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults;
import ch.vd.uniregctb.document.EnvoiDIsRapport;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamLong;

/**
 * Job qui envoie à l'impression les déclaration d'impôts (à partir des tâches en instance)
 */
public class EnvoiDIsJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;

	public static final String NAME = "EnvoiDIsEnMasseJob";
	private static final String CATEGORIE = "DI";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String CATEGORIE_CTB = "CATEGORIE";
	public static final String NB_MAX = "NB_MAX";
	public static final String EXCLURE_DCD = "EXCLURE_DCD";
	public static final String CTB_NO_MIN = "CTB_NO_MIN";
	public static final String CTB_NO_MAX = "CTB_NO_MAX";

	public EnvoiDIsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		{
			final RegDate today = RegDate.get();
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, today.year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Catégorie de contribuables");
			param.setName(CATEGORIE_CTB);
			param.setMandatory(true);
			param.setType(new JobParamEnum(CategorieEnvoiDI.class));
			addParameterDefinition(param, CategorieEnvoiDI.VAUDOIS_COMPLETE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Numéro de contribuable minimum");
			param.setName(CTB_NO_MIN);
			param.setMandatory(false);
			param.setType(new JobParamLong());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Numéro de contribuable maximum");
			param.setName(CTB_NO_MAX);
			param.setMandatory(false);
			param.setType(new JobParamLong());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximum d'envois");
			param.setName(NB_MAX);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 100);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Exclure les décédés de fin d'année");
			param.setName(EXCLURE_DCD);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Récupération des paramètres
		final int annee = getIntegerValue(params, PERIODE_FISCALE);
		final CategorieEnvoiDI categorie = getEnumValue(params, CATEGORIE_CTB, CategorieEnvoiDI.class);

		final Long noCtbMin = getOptionalLongValue(params, CTB_NO_MIN);
		final Long noCtbMax = getOptionalLongValue(params, CTB_NO_MAX);
		if (noCtbMin != null || noCtbMax != null) {
			if (noCtbMin != null && noCtbMin < 0) {
				throw new RuntimeException("Le numéro de contribuable minimum doit être positif.");
			}
			if (noCtbMax != null && noCtbMax < 0) {
				throw new RuntimeException("Le numéro de contribuable maximum doit être positif.");
			}
			if (noCtbMin != null && noCtbMax != null && noCtbMax < noCtbMin) {
				throw new RuntimeException("Le minimum des numéros de contribuable doit être inférieur ou égal au maximum.");
			}
		}

		final int nbMax = getIntegerValue(params, NB_MAX);
		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui
		final boolean exclureDecedes = getBooleanValue(params, EXCLURE_DCD);

		final StatusManager status = getStatusManager();
		final EnvoiDIsResults results = service.envoyerDIsEnMasse(annee, categorie, noCtbMin, noCtbMax, nbMax, dateTraitement, exclureDecedes, status);
		final EnvoiDIsRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);

		final String plageCtb = (noCtbMin == null && noCtbMax == null ? " " : String.format("(plage [%s ; %s]) ", noCtbMin, noCtbMax));
		final StringBuilder builder = new StringBuilder();
		builder.append("L'envoi des DIs en masse");
		builder.append(plageCtb);
		builder.append("pour l'année ");
		builder.append(annee);
		builder.append(" et la catégorie de contribuables ");
		builder.append(categorie.name());
		builder.append(" à la date du ");
		builder.append(RegDateHelper.dateToDisplayString(dateTraitement));
		if (exclureDecedes) {
			builder.append(" avec exclusion des décédés de fin d'année");
		}
		builder.append(" est terminée.");
		Audit.success(builder.toString(), rapport);
	}
}

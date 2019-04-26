package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.EnvoiDIsPPRapport;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamEnum;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamLong;

/**
 * Job qui envoie à l'impression les déclaration d'impôts (à partir des tâches en instance)
 */
public class EnvoiDIsJob extends JobDefinition {

	public static final String NAME = "EnvoiDIsEnMasseJob";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String CATEGORIE_CTB = "CATEGORIE";
	public static final String NB_MAX = "NB_MAX";
	public static final String EXCLURE_DCD = "EXCLURE_DCD";
	public static final String CTB_NO_MIN = "CTB_NO_MIN";
	public static final String CTB_NO_MAX = "CTB_NO_MAX";
	public static final String NB_THREADS = "NB_THREADS";

	private DeclarationImpotService service;
	private RapportService rapportService;

	public EnvoiDIsJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PP, sortOrder, description);

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
			param.setType(new JobParamEnum(CategorieEnvoiDIPP.class));
			addParameterDefinition(param, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);
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
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
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
		final CategorieEnvoiDIPP categorie = getEnumValue(params, CATEGORIE_CTB, CategorieEnvoiDIPP.class);

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
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		final StatusManager status = getStatusManager();
		final EnvoiDIsPPResults results = service.envoyerDIsPPEnMasse(annee, categorie, noCtbMin, noCtbMax, nbMax, dateTraitement, exclureDecedes, nbThreads, status);
		final EnvoiDIsPPRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);

		final String plageCtb = (noCtbMin == null && noCtbMax == null ? " " : String.format("(plage [%s ; %s]) ", noCtbMin, noCtbMax));
		final StringBuilder builder = new StringBuilder();
		builder.append("L'envoi des DIs PP en masse");
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
		builder.append(" est terminé.");
		audit.success(builder.toString(), rapport);
	}
}

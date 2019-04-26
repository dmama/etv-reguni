package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.EnvoiDIsPMRapport;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPM;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamEnum;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class EnvoiDIsPMJob extends JobDefinition {

	private static final String NAME = "EnvoiDIsPMEnMasseJob";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String TYPE_DI = "TYPE_DI";
	public static final String NB_MAX_ENVOIS = "NB_MAX_ENVOIS";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String DATE_LIMITE_BOUCLEMENT = "LIMITE_BOUCLEMENT";

	private DeclarationImpotService service;
	private RapportService rapportService;

	public EnvoiDIsPMJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PM, sortOrder, description);

		final RegDate today = RegDate.get();
		final RegDate endOfLastTrimester = getEndOfTrimester(today).addMonths(-3);
		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, endOfLastTrimester.year());
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Type d'envoi");
			param.setName(TYPE_DI);
			param.setMandatory(true);
			param.setType(new JobParamEnum(CategorieEnvoiDIPM.class));
			addParameterDefinition(param, CategorieEnvoiDIPM.DI_PM);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date limite des bouclements pris en compte");
			param.setName(DATE_LIMITE_BOUCLEMENT);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, endOfLastTrimester);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximum d'envois");
			param.setName(NB_MAX_ENVOIS);
			param.setMandatory(false);
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

	/**
	 * @param month le mois de la date de référence (1..12)
	 * @return le mois de la fin du trimestre de la date de référence (1..12)
	 */
	protected static int getMonthAtEndOfTrimester(int month) {
		return ((month - 1) / 3 + 1) * 3;
	}

	/**
	 * @param date une date de référence
	 * @return le jour de la fin du trimestre correspondant à la date de référence
	 */
	protected static RegDate getEndOfTrimester(RegDate date) {
		return RegDate.get(date.year(), getMonthAtEndOfTrimester(date.month()), 1).getLastDayOfTheMonth();
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
		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final CategorieEnvoiDIPM categorieEnvoi = getEnumValue(params, TYPE_DI, CategorieEnvoiDIPM.class);
		final Integer nbMaxEnvois = getOptionalIntegerValue(params, NB_MAX_ENVOIS);
		final RegDate dateTraitement = getDateTraitement(params);
		final RegDate dateLimiteBouclements = getRegDateValue(params, DATE_LIMITE_BOUCLEMENT);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		if (dateTraitement.isBefore(dateLimiteBouclements)) {
			throw new IllegalArgumentException(String.format("La valeur du paramètre '%s' (%s) doit être antérieure à la date de traitement (%s).",
			                                                 DATE_LIMITE_BOUCLEMENT,
			                                                 RegDateHelper.dateToDisplayString(dateLimiteBouclements),
			                                                 RegDateHelper.dateToDisplayString(dateTraitement)));
		}

		final StatusManager status = getStatusManager();
		final EnvoiDIsPMResults results = service.envoyerDIsPMEnMasse(pf, categorieEnvoi, dateLimiteBouclements, nbMaxEnvois, dateTraitement, nbThreads, status);
		final EnvoiDIsPMRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);

		final StringBuilder builder = new StringBuilder();
		builder.append("L'envoi des DIs PM en masse pour la période fiscale ");
		builder.append(pf);
		builder.append(" et la type de document ");
		builder.append(categorieEnvoi.name());
		builder.append(" à la date du ");
		builder.append(RegDateHelper.dateToDisplayString(dateTraitement));
		builder.append(" est terminé.");
		audit.success(builder.toString(), rapport);

	}
}

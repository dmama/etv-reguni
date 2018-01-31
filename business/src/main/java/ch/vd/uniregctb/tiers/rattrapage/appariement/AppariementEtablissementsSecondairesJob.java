package ch.vd.uniregctb.tiers.rattrapage.appariement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.AppariementEtablissementsSecondairesRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamString;
import ch.vd.uniregctb.tiers.TiersService;

public class AppariementEtablissementsSecondairesJob extends JobDefinition {

	private static final String NAME = "AppariementEtablissementsSecondairesJob";

	private static final String PARAM_SIMULATION = "SIMULATION";
	private static final String PARAM_ENTREPRISE_IDS = "ENTREPRISES";
	private static final String PARAM_THREADS = "NB_THREADS";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private AppariementService appariementService;
	private TiersService tiersService;
	private Dialect dbDialect;
	private RapportService rapportService;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAppariementService(AppariementService appariementService) {
		this.appariementService = appariementService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setDbDialect(Dialect dbDialect) {
		this.dbDialect = dbDialect;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public AppariementEtablissementsSecondairesJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(PARAM_THREADS);
			param.setMandatory(true);
			param.setEnabled(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Mode simulation");
			param.setName(PARAM_SIMULATION);
			param.setMandatory(false);
			param.setEnabled(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Entreprises à traiter (vide = toutes, sinon identifiants séparés par des espaces, virgules ou points-virgules)");
			param.setName(PARAM_ENTREPRISE_IDS);
			param.setMandatory(false);
			param.setEnabled(true);
			param.setType(new JobParamString());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getPositiveIntegerValue(params, PARAM_THREADS);
		final boolean simulation = getBooleanValue(params, PARAM_SIMULATION);
		final List<Long> ids = extractIds(getStringValue(params, PARAM_ENTREPRISE_IDS));

		// tentatives d'appariements
		final StatusManager statusManager = getStatusManager();
		final AppariementEtablissementsSecondairesProcessor processor = new AppariementEtablissementsSecondairesProcessor(transactionManager, hibernateTemplate, appariementService, tiersService, dbDialect);
		final AppariementEtablissementsSecondairesResults results;
		if (ids == null) {
			results = processor.run(nbThreads, simulation, statusManager);
		}
		else {
			results = processor.run(ids, nbThreads, simulation, statusManager);
		}

		// génération du pdf et fin
		final AppariementEtablissementsSecondairesRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);
	}

	/**
	 * @param ids une chaîne de caractères représentant une liste d'identifiants d'entreprises séparés par des espaces, virgules ou points-virgules
	 * @return la liste des identifiants reconnus, <code>null</code> si liste vide
	 * @throws IllegalArgumentException si la chaîne de caractères n'est pas interprétable
	 */
	@Nullable
	private static List<Long> extractIds(String ids) {
		final Pattern longPattern = Pattern.compile("[0-9]{1,8}");
		if (StringUtils.isBlank(ids)) {
			return null;
		}
		final String[] strIds = ids.split("[ ;,]");
		if (strIds.length > 0) {
			final List<Long> list = new ArrayList<>(strIds.length);
			for (String str : strIds) {
				if (StringUtils.isNotBlank(str)) {
					final Matcher matcher = longPattern.matcher(str);
					if (matcher.matches()) {
						list.add(Long.parseLong(str));
					}
					else {
						throw new IllegalArgumentException("Valeur invalide : '" + str + "'");
					}
				}
			}
			return list.isEmpty() ? null : list;
		}
		else {
			return null;
		}
	}
}

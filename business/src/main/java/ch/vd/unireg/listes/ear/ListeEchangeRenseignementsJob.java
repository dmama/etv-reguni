package ch.vd.unireg.listes.ear;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ListeEchangeRenseignementsRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class ListeEchangeRenseignementsJob extends JobDefinition {

	private static final String NAME = "ListeEchangeRenseignementsJob";

	private static final String PERIODE_FISCALE = "PERIODE";
	private static final String NB_THREADS = "NB_THREADS";
	public static final String B_CONTRIBUABLES_PP = "avecContribuablesPP";
	public static final String B_CONTRIBUABLES_PM = "avecContribuablesPM";

	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private RapportService rapportService;
	private AssujettissementService assujettissementService;
	private AdresseService adresseService;

	public ListeEchangeRenseignementsJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

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
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}

		final JobParam param2 = new JobParam();
		param2.setDescription("Inclure les personnes physiques / ménages");
		param2.setName(B_CONTRIBUABLES_PP);
		param2.setMandatory(true);
		param2.setType(new JobParamBoolean());
		addParameterDefinition(param2, Boolean.TRUE);


		final JobParam param4 = new JobParam();
		param4.setDescription("Inclure les personnes morales");
		param4.setName(B_CONTRIBUABLES_PM);
		param4.setMandatory(true);
		param4.setType(new JobParamBoolean());
		addParameterDefinition(param4, Boolean.TRUE);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();
		// population à lister ?
		final boolean avecContribuablesPP = getBooleanValue(params, B_CONTRIBUABLES_PP);
		final boolean avecContribuablesPM = getBooleanValue(params, B_CONTRIBUABLES_PM);

		final ListeEchangeRenseignementsProcessor proc = new ListeEchangeRenseignementsProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO, assujettissementService, adresseService);
		final ListeEchangeRenseignementsResults results = proc.run(dateTraitement, nbThreads, pf, avecContribuablesPP, avecContribuablesPM, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListeEchangeRenseignementsRapport rapport = template.execute(status -> rapportService.generateRapport(results, statusManager));

		setLastRunReport(rapport);
		Audit.success("La production de la liste pour l'échanche automatique de renseignements (EAR) " + pf + " est terminée.", rapport);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}

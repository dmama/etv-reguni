package ch.vd.unireg.listes.assujettis;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.ListeAssujettisRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class ListeAssujettisJob extends JobDefinition {

	private static final String NAME = "ListeAssujettisJob";

	private static final String PERIODE_FISCALE = "PERIODE";
	private static final String NB_THREADS = "NB_THREADS";
	private static final String SOURCIERS_PURS = "SOURCIERS_PURS";
	private static final String FIN_ANNEE_SEULEMENT = "FIN_ANNEE_SEULEMENT";
	private static final String LISTE_CTBS = "LISTE_CTBS";

	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private RapportService rapportService;
	private AssujettissementService assujettissementService;
	private AdresseService adresseService;

	public ListeAssujettisJob(int sortOrder, String description) {
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
		{
			final JobParam param = new JobParam();
			param.setDescription("Inclure les sourciers purs");
			param.setName(SOURCIERS_PURS);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, true);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Seulement les assujettis à la fin de l'année");
			param.setName(FIN_ANNEE_SEULEMENT);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, false);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Liste des contribuables à inspecter");
			param.setName(LISTE_CTBS);
			param.setMandatory(false);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		final boolean avecSrcPurs = getBooleanValue(params, SOURCIERS_PURS);
		final boolean seultAssujettisFinAnnee = getBooleanValue(params, FIN_ANNEE_SEULEMENT);
		final List<Long> ctbs = extractIdsFromCSV(getFileContent(params, LISTE_CTBS));
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();

		final ListeAssujettisProcessor proc = new ListeAssujettisProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO, assujettissementService, adresseService);
		final ListeAssujettisResults results = proc.run(dateTraitement, nbThreads, pf, avecSrcPurs, seultAssujettisFinAnnee, ctbs, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListeAssujettisRapport rapport = template.execute(status -> rapportService.generateRapport(results, statusManager));

		setLastRunReport(rapport);
		audit.success("La production de la liste des assujettis " + pf + " est terminée.", rapport);
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

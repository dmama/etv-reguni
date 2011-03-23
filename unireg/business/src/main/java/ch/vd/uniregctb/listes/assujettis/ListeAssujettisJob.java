package ch.vd.uniregctb.listes.assujettis;

import java.util.Map;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ListeAssujettisRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class ListeAssujettisJob extends JobDefinition {

	private static final String NAME = "ListeAssujettisJob";
	private static final String CATEGORIE = "Stats";

	private static final String PERIODE_FISCALE = "PERIODE";
	private static final String NB_THREADS = "NB_THREADS";
	private static final String SOURCIERS_PURS = "SOURCIERS_PURS";
	private static final String FIN_ANNEE_SEULEMENT = "FIN_ANNEE_SEULEMENT";

	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private RapportService rapportService;

	public ListeAssujettisJob(int sortOrder, String description) {
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
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		final boolean avecSrcPurs = getBooleanValue(params, SOURCIERS_PURS);
		final boolean seultAssujettisFinAnnee = getBooleanValue(params, FIN_ANNEE_SEULEMENT);
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();

		final ListeAssujettisProcessor proc = new ListeAssujettisProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO);
		final ListeAssujettisResults results = proc.run(dateTraitement, nbThreads, pf, avecSrcPurs, seultAssujettisFinAnnee, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ListeAssujettisRapport rapport = (ListeAssujettisRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success("La production de la liste des assujettis " + pf + " est terminée.", rapport);
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
}

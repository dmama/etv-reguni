package ch.vd.uniregctb.metier.piis;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.document.DumpPeriodesImpositionImpotSourceRapport;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;

public class DumpPeriodesImpositionImpotSourceJob extends JobDefinition {

	private static final String NAME = "DumpPeriodesImpositionImpotSourceJob";

	private static final String NB_THREADS = "NB_THREADS";

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private PeriodeImpositionImpotSourceService piisService;
	private RapportService rapportService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setPiisService(PeriodeImpositionImpotSourceService piisService) {
		this.piisService = piisService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public DumpPeriodesImpositionImpotSourceJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final StatusManager sm = getStatusManager();

		final List<Long> ppIds = getIdentifiantsATraiter(sm);
		final DumpPeriodesImpositionImpotSourceResults results = doJob(ppIds, nbThreads);

		// Exécution du rapport dans une transaction.
		final TransactionTemplate rapportTemplate = new TransactionTemplate(transactionManager);
		final DumpPeriodesImpositionImpotSourceRapport rapport = rapportTemplate.execute(new TransactionCallback<DumpPeriodesImpositionImpotSourceRapport>() {
			@Override
			public DumpPeriodesImpositionImpotSourceRapport doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, sm);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		setLastRunReport(rapport);
		Audit.success("Le dump des périodes d'imposition IS est terminé.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}

	private List<Long> getIdentifiantsATraiter(StatusManager sm) {
		sm.setMessage("Récupération des identifiants des personnes physiques de la base de données");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return getIdsPersonnesPhysiques();
			}
		});
	}

	private List<Long> getIdsPersonnesPhysiques() {
		return hibernateTemplate.execute(FlushMode.MANUAL, new HibernateCallback<List<Long>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("select pp.numero from PersonnePhysique pp order by pp.numero");
				return query.list();
			}
		});
	}

	private DumpPeriodesImpositionImpotSourceResults doJob(List<Long> ppIds, final int nbThreads) {
		final StatusManager sm = getStatusManager();
		final DumpPeriodesImpositionImpotSourceResults results = new DumpPeriodesImpositionImpotSourceResults(nbThreads);
		if (!sm.interrupted()) {
			final String msg = "Calcul des périodes d'imposition IS";
			sm.setMessage(msg, 0);

			final ParallelBatchTransactionTemplateWithResults<Long, DumpPeriodesImpositionImpotSourceResults> batchTemplate
					= new ParallelBatchTransactionTemplateWithResults<>(ppIds, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, sm, AuthenticationInterface.INSTANCE);
			batchTemplate.setReadonly(true);

			final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
			batchTemplate.execute(results, new BatchWithResultsCallback<Long, DumpPeriodesImpositionImpotSourceResults>() {
				@Override
				public boolean doInTransaction(List<Long> batch, DumpPeriodesImpositionImpotSourceResults rapport) throws Exception {
					for (Long id : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(id);
						if (pp.isAnnule()) {
							rapport.addIgnore(id, DumpPeriodesImpositionImpotSourceResults.CauseIgnorance.ANNULE);
						}
						else {
							try {
								final List<PeriodeImpositionImpotSource> piis = piisService.determine(pp);
								rapport.addPeriodes(id, piis);
							}
							catch (PeriodeImpositionImpotSourceServiceException e) {
								rapport.addErrorException(id, e);
							}
						}
					}
					sm.setMessage(msg, progressMonitor.getProgressInPercent());
					return !sm.interrupted();
				}

				@Override
				public DumpPeriodesImpositionImpotSourceResults createSubRapport() {
					return new DumpPeriodesImpositionImpotSourceResults(nbThreads);
				}

			}, progressMonitor);
		}

		if (sm.interrupted()) {
			sm.setMessage("Le calcul des périodes d'imposition IS a été interrompu.");
			results.setInterrupted(true);
		}
		else {
			sm.setMessage("Le calcul des périodes d'impositions IS est terminé.");
		}
		results.end();
		return results;
	}
}

package ch.vd.uniregctb.tiers.jobs;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class CorrectionForsHCJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(CorrectionForsHCJob.class);
	
	public static final String NAME = "CorrectionForsHCJob";
	
	private static final String CATEGORIE = "Database";
	
	public static final int BATCH_SIZE = 20;
	
	private PlatformTransactionManager transactionManager;
	
	private HibernateTemplate hibernateTemplate;

	private TiersService tiersService;
	
	public CorrectionForsHCJob(int order, String description) {
		super(NAME, CATEGORIE, order, description);
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setMetierService(MetierService metierService) {
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		
		final StatusManager statusManager;
		if (getStatusManager() == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		else {
			statusManager = getStatusManager();
		}
		
		Audit.success("Demarrage du traitement de correction de fors HC.");
			
		final List<Long> ids = retrieveIdFors();
		
		final BatchTransactionTemplate<Long, JobResults> t = new BatchTransactionTemplate<Long, JobResults>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, hibernateTemplate);
		t.execute(null, createProcessor(ids, statusManager));
		Audit.success("Traitement de correction de fors HC terminé.");
	}

	private BatchCallback<Long, JobResults> createProcessor(List<Long> ids, StatusManager statusManager) {
		final CorrectionForsProcessor processor = new CorrectionForsProcessor();
		processor.setForIds(ids);
		processor.setStatusManager(statusManager);
		return processor;
	}

	@SuppressWarnings("unchecked")
	private List<Long> retrieveIdFors() {
		
		final String queryStr= "select ffp.id from ForFiscalPrincipal as ffp"
			+ " where ffp.typeAutoriteFiscale!='COMMUNE_OU_FRACTION_VD'"
			+ " and ffp.dateFin is null and ffp.annulationDate is null"
			+ " and ffp.tiers.class=MenageCommun";
		
		final List ids = hibernateTemplate.find(queryStr);
		return ids;
		
	}

	@SuppressWarnings("unchecked")
	private Set<ForFiscalPrincipal> getFors(final List<Long> ids) {
		
		final List<ForFiscalPrincipal> list = (List<ForFiscalPrincipal>) hibernateTemplate.executeWithNativeSession(new HibernateCallback(){

			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(ForFiscalPrincipal.class);
				criteria.add(Restrictions.in("id", ids));
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				
				final FlushMode mode = session.getFlushMode();
				try {
					session.setFlushMode(FlushMode.MANUAL);
					return criteria.list();
				}
				finally {
					session.setFlushMode(mode);
				}
			}
			
		});
		
		return new HashSet<ForFiscalPrincipal>(list);
	}

	public boolean isForVaudois(ForFiscalPrincipal ffp) {
		if (ffp == null) {
			return false;
		}
		if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == ffp.getTypeAutoriteFiscale()) {
			return true;
		}
		return false;
	}
	
	class CorrectionForsProcessor extends BatchCallback<Long, JobResults> {
		
		private int currentBatch = 0;
		private int analyses = 0;
		private int rouverts = 0;
		private int erreurs = 0;
		
		private StatusManager statusManager;
		private List<Long> forIds;
		
		@Override
		public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
			
			AuthenticationHelper.pushPrincipal("[" + NAME + " " + AuthenticationHelper.getCurrentPrincipal() + "]");
			try {
				
				final Set<ForFiscalPrincipal> fors = getFors(batch);
				for (ForFiscalPrincipal ffp : fors) {
					Audit.info(String.format("Analyse du for %d", ffp.getId()));
					
					MenageCommun menage = (MenageCommun) ffp.getTiers();
					final RegDate dateDebutFor = ffp.getDateDebut();
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, dateDebutFor);
					
					final PersonnePhysique principal = couple.getPrincipal();
					final PersonnePhysique conjoint = couple.getConjoint();
					
					if (conjoint == null) {
						Audit.info("Marié seul détecté -> à vérifier manuellement");
					}
					else {
						final RegDate veuilleDebutFor = dateDebutFor.getOneDayBefore();
						final ForFiscalPrincipal ffpPrincipal = principal.getForFiscalPrincipalAt(veuilleDebutFor);
						final ForFiscalPrincipal ffpConjoint;
						if (conjoint != null) {
							ffpConjoint = conjoint.getForFiscalPrincipalAt(veuilleDebutFor);
						}
						else {
							ffpConjoint = null;
						}
						
						if (ffpPrincipal == null || ffpConjoint == null) {
							Audit.info("Aucun ou seulement un des deux membres possédait un for -> à vérifier manuellement");
						}
						else
						{
							final boolean isForDuPrincipalVaudois = isForVaudois(ffpPrincipal);
							final boolean isForDuConjointVaudois = isForVaudois(ffpConjoint);
							
							if (!isForDuPrincipalVaudois && isForDuConjointVaudois) {
								Audit.info(String.format("Rattachement du for à la commune # %d", ffpConjoint.getNumeroOfsAutoriteFiscale()));
								ForFiscalPrincipal nouveauFor = (ForFiscalPrincipal) ffp.duplicate();
								// annuler le for car il est pas rattaché à la bonne commune
								tiersService.annuleForFiscal(ffp, true);
								// ouvrir une nouveau for rattaché à la bonne commune
								nouveauFor.setTypeAutoriteFiscale(ffpConjoint.getTypeAutoriteFiscale());
								nouveauFor.setNumeroOfsAutoriteFiscale(ffpConjoint.getNumeroOfsAutoriteFiscale());
								
								menage.addForFiscal(nouveauFor);
								menage = (MenageCommun) tiersService.getTiersDAO().save(menage);
								rouverts++;
							}
							else {
								Audit.info("Les deux fors sont vaudois -> à vérifier manuellement");
							}
						}
					}
					analyses++;
					Audit.info(String.format("Analyse du for %d terminé", ffp.getId()));
				}
			}
			catch (RuntimeException e) {
				erreurs++;
				LOGGER.error(e.getMessage(), e);
				throw e;
			}
			catch (Exception e) {
				erreurs++;
				LOGGER.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
				LOGGER.debug("Batch # " + currentBatch + " terminé");
			}
			return !statusManager.interrupted();
		}
		
		@Override
		public void afterTransactionCommit() {
			int percent = (100 * analyses) / forIds.size();
			final String message = String.format("%d sur %d des for hors canton analysés (%d%%) : %d rouverts, %d en erreur", analyses, forIds.size(), percent, rouverts, erreurs);
			statusManager.setMessage(message);
			Audit.info(message);
		}

		public StatusManager getStatusManager() {
			return statusManager;
		}

		public void setStatusManager(StatusManager statusManager) {
			this.statusManager = statusManager;
		}

		public List<Long> getForIds() {
			return forIds;
		}

		public void setForIds(List<Long> forIds) {
			this.forIds = forIds;
		}
		
	}
}

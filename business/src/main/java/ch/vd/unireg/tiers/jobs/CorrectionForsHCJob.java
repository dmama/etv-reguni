package ch.vd.unireg.tiers.jobs;

import javax.persistence.FlushModeType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.BatchTransactionTemplate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class CorrectionForsHCJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrectionForsHCJob.class);
	public static final String NAME = "CorrectionForsHCJob";
	public static final int BATCH_SIZE = 20;
	
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;

	public CorrectionForsHCJob(int order, String description) {
		super(NAME, JobCategory.DB, order, description);
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	private static final class CorrectionMonitor extends SimpleProgressMonitor {
		private int analyses = 0;
		private int rouverts = 0;
		private int erreurs = 0;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		
		final StatusManager statusManager = getStatusManager();
		audit.success("Demarrage du traitement de correction de fors HC.");
			
		final List<Long> ids = retrieveIdFors();
		final CorrectionMonitor monitor = new CorrectionMonitor();
		final BatchTransactionTemplate<Long> t = new BatchTransactionTemplate<>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager);
		t.execute(new BatchCallback<Long>() {
			@Override
			public boolean doInTransaction(List<Long> batch) {
				return doBatch(batch, monitor);
			}

			@Override
			public void afterTransactionCommit() {
				super.afterTransactionCommit();

				final String message = String.format("%d sur %d des for hors canton analysés (%d%%) : %d rouverts, %d en erreur",
				                                     monitor.analyses, ids.size(), monitor.getProgressInPercent(), monitor.rouverts, monitor.erreurs);
				getStatusManager().setMessage(message);
				audit.info(message);
			}

		}, monitor);

		audit.success("Traitement de correction de fors HC terminé.");
	}

	private List<Long> retrieveIdFors() {
		
		final String queryStr= "select ffp.id from ForFiscalPrincipalPP as ffp"
			+ " join ffp.tiers tiers"
			+ " where ffp.typeAutoriteFiscale!='COMMUNE_OU_FRACTION_VD'"
			+ " and ffp.dateFin is null and ffp.annulationDate is null"
			+ " and type(tiers)=MenageCommun";
		
		return hibernateTemplate.find(queryStr, null);
	}

	@SuppressWarnings("unchecked")
	private Set<ForFiscalPrincipal> getFors(final List<Long> ids) {
		
		final List<ForFiscalPrincipal> list = hibernateTemplate.execute(session -> {
			final FlushModeType mode = session.getFlushMode();
			try {
				session.setFlushMode(FlushModeType.COMMIT);
				final Query query = session.createQuery("from ForFiscalPrincipal where id in (:ids)");
				query.setParameterList("ids", ids);
				return query.list();
			}
			finally {
				session.setFlushMode(mode);
			}
		});
		
		return new HashSet<>(list);
	}

	public boolean isForVaudois(ForFiscalPrincipal ffp) {
		return ffp != null && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == ffp.getTypeAutoriteFiscale();
	}

	private boolean doBatch(List<Long> ids, CorrectionMonitor monitor) {
		try {
			final Set<ForFiscalPrincipal> fors = getFors(ids);
			for (ForFiscalPrincipal ffp : fors) {
				audit.info(String.format("Analyse du for %d", ffp.getId()));

				MenageCommun menage = (MenageCommun) ffp.getTiers();
				final RegDate dateDebutFor = ffp.getDateDebut();
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, dateDebutFor);

				final PersonnePhysique principal = couple.getPrincipal();
				final PersonnePhysique conjoint = couple.getConjoint();

				if (conjoint == null) {
					audit.info("Marié seul détecté -> à vérifier manuellement");
				}
				else {
					final RegDate veuilleDebutFor = dateDebutFor.getOneDayBefore();
					final ForFiscalPrincipal ffpPrincipal = principal.getForFiscalPrincipalAt(veuilleDebutFor);
					final ForFiscalPrincipal ffpConjoint = conjoint.getForFiscalPrincipalAt(veuilleDebutFor);

					if (ffpPrincipal == null || ffpConjoint == null) {
						audit.info("Aucun ou seulement un des deux membres possédait un for -> à vérifier manuellement");
					}
					else
					{
						final boolean isForDuPrincipalVaudois = isForVaudois(ffpPrincipal);
						final boolean isForDuConjointVaudois = isForVaudois(ffpConjoint);

						if (!isForDuPrincipalVaudois && isForDuConjointVaudois) {
							audit.info(String.format("Rattachement du for à la commune # %d", ffpConjoint.getNumeroOfsAutoriteFiscale()));
							ForFiscalPrincipal nouveauFor = (ForFiscalPrincipal) ffp.duplicate();
							// annuler le for car il est pas rattaché à la bonne commune
							tiersService.annuleForFiscal(ffp);
							// ouvrir une nouveau for rattaché à la bonne commune
							nouveauFor.setTypeAutoriteFiscale(ffpConjoint.getTypeAutoriteFiscale());
							nouveauFor.setNumeroOfsAutoriteFiscale(ffpConjoint.getNumeroOfsAutoriteFiscale());

							menage.addForFiscal(nouveauFor);
							menage = hibernateTemplate.merge(menage);

							++ monitor.rouverts;
						}
						else {
							audit.info("Les deux fors sont vaudois -> à vérifier manuellement");
						}
					}
				}

				++ monitor.analyses;
				audit.info(String.format("Analyse du for %d terminé", ffp.getId()));
			}

			return !getStatusManager().isInterrupted();
		}
		catch (RuntimeException e) {
			++ monitor.erreurs;
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		catch (Exception e) {
			++ monitor.erreurs;
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}

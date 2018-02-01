package ch.vd.unireg.listes.assujettis;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.AssujettissementParSubstitution;
import ch.vd.unireg.tiers.TiersService;

public class AssujettisParSubstitutionProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssujettisParSubstitutionProcessor.class);
	private static final int TAILLE_LOT = 10;

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final AssujettissementService assujettissementService;
	private final PlatformTransactionManager transactionManager;

	public AssujettisParSubstitutionProcessor(HibernateTemplate hibernateTemplate, TiersService tiersService,
	                                          PlatformTransactionManager transactionManager, AssujettissementService assujettissementService) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.assujettissementService = assujettissementService;
		this.transactionManager = transactionManager;
	}

	public AssujettisParSubstitutionResults run(final RegDate dateTraitement, final int nbThreads, StatusManager s) throws Exception {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		AssujettisParSubstitutionResults rapportFinal = new AssujettisParSubstitutionResults(dateTraitement,nbThreads,tiersService,assujettissementService);
		status.setMessage("Récupération des assujettissement par substitution ...");
		final List<Long> ids = getIdRapportSubstitution();
		if (ids != null && !ids.isEmpty()) {


			final String messageTraitement = String.format("Traitement des %d rapports de substitution trouvés.", ids.size());

			final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
			final ParallelBatchTransactionTemplateWithResults<Long, AssujettisParSubstitutionResults> template =
					new ParallelBatchTransactionTemplateWithResults<>(ids, TAILLE_LOT, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager,
					                                                  status, AuthenticationInterface.INSTANCE);

			// et on y va !
			template.execute(rapportFinal, new BatchWithResultsCallback<Long, AssujettisParSubstitutionResults>() {
				@Override
				public boolean doInTransaction(List<Long> batch, AssujettisParSubstitutionResults rapport) throws Exception {
					status.setMessage(messageTraitement, progressMonitor.getProgressInPercent());
					traiterBatch(batch,rapport);
					return true;
				}

				@Override
				public AssujettisParSubstitutionResults createSubRapport() {
					return new AssujettisParSubstitutionResults(dateTraitement,nbThreads,tiersService,assujettissementService);
				}

			}, progressMonitor);
		}

		if (status.isInterrupted()) {
			rapportFinal.setInterrupted(true);
			status.setMessage("Traitement interrompu.");
		}
		else {
			status.setMessage("Traitement terminé.");
		}
		rapportFinal.end();
		return rapportFinal;

	}

	@SuppressWarnings({"unchecked"})
	private List<Long> getIdRapportSubstitution(){
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final StringBuilder b = new StringBuilder();
				b.append("SELECT DISTINCT rapport.id FROM AssujettissementParSubstitution AS rapport");
				b.append(" WHERE rapport.annulationDate IS NULL");
				b.append(" AND rapport.annulationUser IS NULL");
				b.append(" ORDER BY rapport.id ASC");
				final String hql = b.toString();
				return hibernateTemplate.find(hql, null);
			}
		});

	}

	private void traiterBatch(List<Long> batch, AssujettisParSubstitutionResults results) throws Exception {
		for (Long id : batch) {
			final AssujettissementParSubstitution rapportAssuj = hibernateTemplate.get(AssujettissementParSubstitution.class,id);
			results.addInfosRapports(rapportAssuj);
		}
	}


}

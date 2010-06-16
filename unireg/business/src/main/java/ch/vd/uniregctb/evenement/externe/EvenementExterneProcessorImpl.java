package ch.vd.uniregctb.evenement.externe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;

public class EvenementExterneProcessorImpl implements EvenementExterneProcessor {
	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(EvenementExterneProcessorImpl.class);
	private int batchSize = BATCH_SIZE;
	private EvenementExterneDAO evenementExterneDAO;
	private EvenementExterneService evenementExterneService;
	private PlatformTransactionManager transactionManager;
	private final ThreadLocal<TraiterEvenementExterneResult> rapport = new ThreadLocal<TraiterEvenementExterneResult>();

	public TraiterEvenementExterneResult traiteEvenementExterne(final RegDate dateTraitement, int nbThreads, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final TraiterEvenementExterneResult rapportFinal = new TraiterEvenementExterneResult(dateTraitement);
		status.setMessage("Récupération des evenements externes à traiter...");
		final List<Long> ids = recupererEvenementATraiter();

		// Reussi les messages par lots
		final ParallelBatchTransactionTemplate<Long, TraiterEvenementExterneResult>
				template = new ParallelBatchTransactionTemplate<Long, TraiterEvenementExterneResult>(ids, batchSize, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, evenementExterneDAO.getHibernateTemplate());
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, TraiterEvenementExterneResult>() {

			@Override
			public TraiterEvenementExterneResult createSubRapport() {
				return new TraiterEvenementExterneResult(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, TraiterEvenementExterneResult r) throws Exception {

				rapport.set(r);
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch);
				return true;
			}
		});

		final int count = rapportFinal.nbEvenementTotal;

		if (status.interrupted()) {
			status.setMessage("la relance du traitement des evenements externes a été interrompue."
					+ " Nombre d'evenement traités au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("la relance  relance du traitement des evenements externes est terminée."
					+ "Nombre d'evenement total parcouru = " + rapportFinal.nbEvenementTotal + ". Nombre d'evenement traites = " + rapportFinal.traites.size() + ". Nombre d'erreurs = " +
					rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;
	}

	private void traiterBatch(final List<Long> batch) throws EvenementExterneException {
		//Chargement des evenement externes		
		final List<EvenementExterne> list = (List<EvenementExterne>) evenementExterneDAO.getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Criteria crit = session.createCriteria(EvenementExterne.class);
				crit.add(Restrictions.in("id", batch));
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return crit.list();
			}
		});
		for (EvenementExterne evenementExterne : list) {
			rapport.get().nbEvenementTotal++;

			boolean estTraite = evenementExterneService.traiterEvenementExterne(evenementExterne);
			if (estTraite) {
				rapport.get().addTraite(evenementExterne);
			}
			else {
				rapport.get().addIgnores(evenementExterne);
			}
		}


	}

	private List<Long> recupererEvenementATraiter() {
		final String queryMessage ="select distinct evenementExterne.id from EvenementExterne evenementExterne where evenementExterne.etat =:etat";


		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<Long> idsEvenement = (List<Long>) evenementExterneDAO.getHibernateTemplate().executeWithNewSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						Query queryObject = session.createQuery(queryMessage);
						queryObject.setParameter("etat", EtatEvenementExterne.ERREUR.name());
						return queryObject.list();
					}
				});
				Collections.sort(idsEvenement);
				return idsEvenement;
			}

		});
		return ids;
	}

	public void setEvenementExterneDAO(EvenementExterneDAO evenementExterneDAO) {
		this.evenementExterneDAO = evenementExterneDAO;
	}

	public void setEvenementExterneService(EvenementExterneService evenementExterneService) {
		this.evenementExterneService = evenementExterneService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
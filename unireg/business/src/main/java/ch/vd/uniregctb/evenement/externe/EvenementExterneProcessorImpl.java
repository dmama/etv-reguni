package ch.vd.uniregctb.evenement.externe;

import java.util.EnumSet;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;
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
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementExterneProcessorImpl implements EvenementExterneProcessor {

	private static final int BATCH_SIZE = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementExterneProcessorImpl.class);

	private HibernateTemplate hibernateTemplate;
	private EvenementExterneService evenementExterneService;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private AdresseService adresseService;

	@Override
	public TraiterEvenementExterneResult traiteEvenementsExternes(final RegDate dateTraitement, int nbThreads, @Nullable StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final TraiterEvenementExterneResult rapportFinal = new TraiterEvenementExterneResult(dateTraitement, tiersService, adresseService);
		status.setMessage("Récupération des événements externes à traiter...");
		final List<Long> ids = recupererEvenementATraiter();

		// Reussi les messages par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, TraiterEvenementExterneResult>
				template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TraiterEvenementExterneResult>() {

			@Override
			public TraiterEvenementExterneResult createSubRapport() {
				return new TraiterEvenementExterneResult(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, TraiterEvenementExterneResult r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, r);
				return true;
			}
		}, progressMonitor);

		final int count = rapportFinal.nbEvenementTotal;

		if (status.interrupted()) {
			status.setMessage("la relance du traitement des evenements externes a été interrompue."
					                  + " Nombre d'evenement traités au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("la relance  relance du traitement des evenements externes est terminée."
					                  + "Nombre d'evenement total parcouru = " + rapportFinal.nbEvenementTotal + ". Nombre d'evenement traites = " + rapportFinal.traites.size() +
					                  ". Nombre d'erreurs = " +
					                  rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;
	}

	private void traiterBatch(final List<Long> batch, TraiterEvenementExterneResult r) throws EvenementExterneException {

		//Chargement des evenement externes
		final List<EvenementExterne> list = hibernateTemplate.execute(new HibernateCallback<List<EvenementExterne>>() {
			@Override
			public List<EvenementExterne> doInHibernate(Session session) throws HibernateException {
				final Criteria crit = session.createCriteria(EvenementExterne.class);
				crit.add(Restrictions.in("id", batch));
				crit.addOrder(Order.asc("id"));
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				//noinspection unchecked
				return crit.list();
			}
		});

		for (EvenementExterne evenementExterne : list) {
			if (evenementExterneService.retraiterEvenementExterne(evenementExterne)) {
				r.addTraite(evenementExterne);
			}
			else {
				r.addErreur(evenementExterne);
			}
		}
	}

	@SuppressWarnings({"UnnecessaryLocalVariable"})
	private List<Long> recupererEvenementATraiter() {
		final String queryMessage = "select evenementExterne.id from EvenementExterne evenementExterne where evenementExterne.etat in (:etats) ORDER BY evenementExterne.id";


		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {

				final List<Long> idsEvenement = hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryMessage);
						queryObject.setParameterList("etats", EnumSet.of(EtatEvenementExterne.ERREUR, EtatEvenementExterne.NON_TRAITE));
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return idsEvenement;
			}

		});
		return ids;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementExterneService(EvenementExterneService evenementExterneService) {
		this.evenementExterneService = evenementExterneService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}
}
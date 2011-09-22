package ch.vd.uniregctb.common;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Classe utilitaire qui permet de découper le processing d'une collection d'éléments en lots (batchs). Chaque lot est exécuté dans une transaction propre.
 * <p/>
 * En cas d'exception levée durant le traitement d'un lot, le comportement est soit une reprise automatique soit pas de reprise (voir {@link Behavior} pour plus de détails).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BatchTransactionTemplate<E, R extends BatchResults> {

	private final Logger LOGGER = Logger.getLogger(BatchTransactionTemplate.class);

	public static enum Behavior {
		/**
		 * En cas d'exception durant le processing d'un batch, considère le batch comme perdu (en erreur, transaction rollée-back) dans son ensemble et continue avec les batchs restants.
		 */
		SANS_REPRISE,
		/**
		 * En cas d'exception durant le processing d'un batch, reprend le processing du batch élément par élément, chacun dans sa propre transaction. A la fin, seuls les éléments posant problèmes n'auront
		 * pas été processés.
		 */
		REPRISE_AUTOMATIQUE
	}

	private final PlatformTransactionManager transactionManager;
	private final BatchIterator<E> iterator;
	private final Behavior behavior;
	private final StatusManager statusManager;
	private final HibernateTemplate hibernateTemplate;

	private boolean readonly;

	/**
	 * @param iterator           un itérateur qui retourne les éléments à processer
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param statusManager      un status manager (peut être nul)
	 * @param hibernateTemplate  le bean hibernateTemplate
	 */
	public BatchTransactionTemplate(Iterator<E> iterator, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager,
	                                StatusManager statusManager, HibernateTemplate hibernateTemplate) {
		this.iterator = new StandardBatchIterator<E>(iterator, batchSize);
		this.behavior = behavior;
		this.transactionManager = transactionManager;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * @param list               la liste des éléments à processer
	 * @param batchSize          la taille maximale des batches
	 * @param behavior           le comportement de l'itérateur en cas d'exception durant la transaction
	 * @param transactionManager le transaction manager Spring
	 * @param statusManager      un status manager (peut être nul)
	 * @param hibernateTemplate  le bean hibernateTemplate
	 */
	public BatchTransactionTemplate(Collection<E> list, int batchSize, Behavior behavior, PlatformTransactionManager transactionManager,
	                                StatusManager statusManager, HibernateTemplate hibernateTemplate) {
		this.iterator = new StandardBatchIterator<E>(list, batchSize);
		this.behavior = behavior;
		this.transactionManager = transactionManager;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	public BatchTransactionTemplate(BatchIterator<E> iterator, Behavior behavior, PlatformTransactionManager transactionManager, StatusManager statusManager,
	                                HibernateTemplate hibernateTemplate) {
		this.transactionManager = transactionManager;
		this.iterator = iterator;
		this.behavior = behavior;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	public static abstract class BatchCallback<E, R extends BatchResults> {

		/**
		 * Le pourcentage de progression du traitement complet des éléments.
		 */
		protected int percent;

		/**
		 * Cette méthode est appelée pour chaque lot avant l'ouverture de la transaction.
		 */
		public void beforeTransaction() {
		}

		/**
		 * Cette méthode est appelée juste après l'ouverture d'une transaction (dans le contexte de celle-ci)
		 * @param status un objet qui permet de monitorer la transaction en cours
		 */
		public void afterTransactionStart(TransactionStatus status) {
		}

		/**
		 * Cette méthode est appelée pour chaque lot. La transaction est ouverte et sera committée à moins qu'une exception soit levée.
		 *
		 * @param batch   le lot à processer
		 * @param rapport le rapport associé au lot courant tel que retourné par {@link #createSubRapport}.
		 * @return true si le prochain lot doit être traité, false si le batch doit être interrompu
		 * @throws Exception si le batch doit être rollé-back
		 */
		public abstract boolean doInTransaction(List<E> batch, R rapport) throws Exception;

		/**
		 * Cette méthode est appelée après que la transaction ait été committée. Elle n'est pas appelée si la transaction a été rollée-back.
		 */
		public void afterTransactionCommit() {
		}

		/**
		 * Cette méthode est appelée après que la transaction ait été rollée-back. Elle n'est pas appelée si la transaction a été committée.
		 *
		 * @param e         l'exception à la source du rollback de la transaction
		 * @param willRetry <code>true</code> si le batch qui vient d'être rollé-back va être repris élément par élément; <code>false</code> autrement.
		 */
		public void afterTransactionRollback(Exception e, boolean willRetry) {
		}

		/**
		 * Cette méthode est appelée avant le processing de chaque nouveau lot. Elle permet de créer un rapport intermédiaire associé au lot. Ce rapport sera automatiquement ajouté au rapport final après le
		 * commit. En cas de rollback avant reprise, le rapport est simplement ignoré. Finalement, en cas de rollback après reprise, l'exception levée est automatiquement ajoutée au rapport final.
		 *
		 * @return une nouveau rapport qui contiendra les résultats de lot courant.
		 */
		public R createSubRapport() {
			return null;
		}
	}

	/**
	 * Exécute le traitement par batchs dans autant de transactions séparées.
	 *
	 * @param action l'action à appeler pour chaque lot devant être traité.
	 * @return <i>true</i> si le traitement s'est bien effectué jusqu'au bout (éventuellement avec des erreurs); ou <i>faux</i> si le traitement a été interrompu.
	 * @throws org.springframework.transaction.TransactionException
	 *          en cas d'erreur de transaction
	 */
	public boolean execute(final BatchCallback<E, R> action) throws TransactionException {
		return execute(null, action);
	}

	/**
	 * Exécute le traitement par batchs dans autant de transactions séparées.
	 *
	 * @param rapportFinal le rapport d'exécution qui sera complété automatiquement.
	 * @param action       l'action à appeler pour chaque lot devant être traité.
	 * @return <i>true</i> si le traitement s'est bien effectué jusqu'au bout (éventuellement avec des erreurs); ou <i>faux</i> si le traitement a été interrompu.
	 * @throws org.springframework.transaction.TransactionException
	 *          en cas d'erreur de transaction
	 */
	public boolean execute(@Nullable final R rapportFinal, final BatchCallback<E, R> action) throws TransactionException {

		final boolean reprise = (behavior == Behavior.REPRISE_AUTOMATIQUE);
		boolean processNextBatch = true;

		action.percent = 0;
		while (iterator.hasNext() && processNextBatch) {

			final List<E> batch = iterator.next();
			if (batch == null) {
				break;
			}

			if (statusManager != null && statusManager.interrupted()) {
				LOGGER.debug("le batch a été interrompu.");
				break;
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("première exécution du batch");
			}

			// exécute le batch en un bloc
			final ExecuteInTransactionResult ret = executeInTransaction(action, batch, reprise, rapportFinal);
			processNextBatch = ret.processNextBatch;

			if (!ret.committed && reprise) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("reprise du batch");
				}

				// re-exécute le batch élément par élément
				for (E e : batch) {
					if (statusManager != null && statusManager.interrupted()) {
						break;
					}

					final List<E> subbatch = new ArrayList<E>(1);
					subbatch.add(e);

					final ExecuteInTransactionResult retReprise = executeInTransaction(action, subbatch, false, rapportFinal);
					if (!retReprise.processNextBatch) {
						break;
					}
				}
			}

			action.percent = iterator.getPercent();
		}

		return processNextBatch;
	}


	/**
	 * Une classe pour stocker le retour de la méthode {@link BatchTransactionTemplate#executeInTransaction(ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback}
	 *
	 * @author xsifnr
	 */
	private class ExecuteInTransactionResult {

		/**
		 * true si la transaction a été committée
		 */
		private boolean committed;


		/**
		 * true si le prochain batch doit être traité
		 */
		private boolean processNextBatch;
	}

	/**
	 * Appelle la méthode doInTransaction sur l'action
	 *
	 * @return la valeur booléenne retournée par la méthode doInTransaction
	 */
	private boolean doInTransaction(BatchCallback<E, R> action, List<E> batch, R rapport) {
		try {
			return action.doInTransaction(batch, rapport);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exécute un batch dans une transaction.
	 *
	 * @return <code>true</code> si le processus s'est bien déroulé et que la transaction est committée; <code>false</code> si la transaction a été rollée-back.
	 */
	private ExecuteInTransactionResult executeInTransaction(final BatchCallback<E, R> action, final List<E> batch, boolean willRetry, @Nullable final R rapportFinal) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Execution en transaction du batch = " + batch);
		}

		final R rapport = action.createSubRapport();

		action.beforeTransaction();

		final ExecuteInTransactionResult r = new ExecuteInTransactionResult();
		r.committed = true;
		r.processNextBatch = true;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(readonly);

		try {
			r.processNextBatch = template.execute(new TransactionCallback<Boolean>() {
				@Override
				public Boolean doInTransaction(TransactionStatus status) {
					action.afterTransactionStart(status);
					return executeWithNewSession(batch, action, rapport);
				}
			});
		}
		catch (final Exception e) {
			r.committed = false;

			if (rapportFinal != null && !willRetry) {
				// le batch ne va pas être rejoué -> on ajoute l'exception
				// on re-crée une transaction ici au cas où on veut étoffer un peu le message d'erreur
				try {
					template.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							addErrorExceptionInNewSession(rapportFinal, batch.get(0), e);
							return null;
						}
					});
				}
				catch (Exception ex) {
					LOGGER.error("Impossible de logger l'erreur suivante dans le rapport final...", e);
					LOGGER.error("... car réception d'une autre erreur au moment de la tentative de log :", ex);
				}
			}
			
			action.afterTransactionRollback(e, willRetry);

			if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn("Rollback du batch", e);
			}
		}

		if (r.committed) {

			if (rapportFinal != null && rapport != null) {
				// on ajoute l'exception directement dans le rapport final
				rapportFinal.addAll(rapport);
			}
			
			action.afterTransactionCommit();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Commit du batch");
			}
		}

		return r;
	}

	private void addErrorExceptionInNewSession(final R rapportFinal, final E elt, final Exception e) {
		hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
			@Override
			@SuppressWarnings({"unchecked"})
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				rapportFinal.addErrorException(elt, e);
				return null;
			}
		});
	}

	/**
	 * Exécute un batch dans une nouvelle session
	 */
	private Boolean executeWithNewSession(final List<E> batch, final BatchCallback<E, R> action, final R rapport) {
		return hibernateTemplate.executeWithNewSession(new HibernateCallback<Boolean>() {
			@Override
			public Boolean doInHibernate(Session session) {
				return BatchTransactionTemplate.this.doInTransaction(action, batch, rapport);
			}
		});
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}

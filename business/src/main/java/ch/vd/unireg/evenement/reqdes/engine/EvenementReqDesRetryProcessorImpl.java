package ch.vd.unireg.evenement.reqdes.engine;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.Fuse;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.reqdes.EtatTraitement;

public class EvenementReqDesRetryProcessorImpl implements EvenementReqDesRetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementReqDesRetryProcessorImpl.class);

	private EvenementReqDesProcessor mainProcessor;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;

	public void setMainProcessor(EvenementReqDesProcessor mainProcessor) {
		this.mainProcessor = mainProcessor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void relancerEvenementsReqDesNonTraites(@Nullable StatusManager s) {

		final StatusManager statusManager = s != null ? s : new LoggingStatusManager(LOGGER);

		final Set<Long> ids = getIds();
		final int originalSize = ids.size();
		if (originalSize > 0) {
			final Fuse done = new Fuse();
			final EvenementReqDesProcessor.ListenerHandle handle = mainProcessor.registerListener(new EvenementReqDesProcessor.Listener() {
				@Override
				public void onUniteTraitee(long idUniteTraitement) {
					ids.remove(idUniteTraitement);
					if (ids.isEmpty()) {
						done.blow();
					}
				}

				@Override
				public void onStop() {
					done.blow();
				}
			});
			try {
				// on envoie la sauce...
				mainProcessor.postUnitesTraitement(ids);

				// .. et on attend la fin
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (done) {
					while (done.isNotBlown() && !statusManager.isInterrupted()) {
						done.wait(1000);

						final int remainingSize = ids.size();
						final int percent = (originalSize - remainingSize) * 100 / originalSize;
						statusManager.setMessage("Traitement en cours...", percent);
					}
				}

				// c'est fini...
				statusManager.setMessage("Traitement terminé.");
			}
			catch (InterruptedException e) {
				// ben on s'arrête...
				LOGGER.warn("Interrupted thread", e);
			}
			finally {
				handle.unregister();
			}
		}
	}

	private Set<Long> getIds() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> hibernateTemplate.executeWithNewSession(new HibernateCallback<Set<Long>>() {
			@Override
			public Set<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("select ut.id from UniteTraitement as ut where ut.etat in (:etats)");
				final Set<EtatTraitement> etats = EnumSet.of(EtatTraitement.A_TRAITER, EtatTraitement.EN_ERREUR);
				query.setParameterList("etats", etats);
				//noinspection unchecked
				return new HashSet<>(query.list());
			}
		}));
	}
}

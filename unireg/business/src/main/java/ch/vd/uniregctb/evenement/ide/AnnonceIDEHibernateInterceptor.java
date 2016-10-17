package ch.vd.uniregctb.evenement.ide;

import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * [SIFISC-9096] Cet intercepteur recalcule automatiquement les parentés sur les personnes physiques connues du civil et modifiées après le commit de chaque transaction.
 */
public class AnnonceIDEHibernateInterceptor implements ModificationSubInterceptor, InitializingBean, Switchable {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDEHibernateInterceptor.class);

	private ModificationInterceptor parent;
	private SessionFactory sessionFactory;
	private TransactionManager transactionManager;
	private Dialect dialect;
	private ServiceIDEService serviceIDEService;

	private final ThreadLocal<HashSet<Long>> modifiedNosEntreprises = new ThreadLocal<HashSet<Long>>() {
		@Override
		protected HashSet<Long> initialValue() {
			return new HashSet<>();
		}
	};

	private final ThreadSwitch activationSwitch = new ThreadSwitch(true);

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		if (entity instanceof RaisonSocialeFiscaleEntreprise) {
			final RaisonSocialeFiscaleEntreprise raisonSociale = (RaisonSocialeFiscaleEntreprise) entity;
			addModifiedEntreprise(raisonSociale.getEntreprise());
		}
		if (entity instanceof FormeJuridiqueFiscaleEntreprise) {
			final FormeJuridiqueFiscaleEntreprise formeJuridique = (FormeJuridiqueFiscaleEntreprise) entity;
			addModifiedEntreprise(formeJuridique.getEntreprise());
		}
		else if (entity instanceof AdresseTiers) {
			final AdresseTiers adresseTiers = (AdresseTiers) entity;
			Tiers tiers = adresseTiers.getTiers();
			if (tiers instanceof Entreprise) {
				addModifiedEntreprise((Entreprise) tiers);
			}
		}
		return false;
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	@Override
	public void preTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionCommit() {
		setDirtyModifiedTiers();
	}

	/**
	 * Met le flag 'dirty' à <i>vrai</i> sur toutes les entreprises modifiées en base de données.
	 */
	private void setDirtyModifiedTiers() {

		final Set<Long> ids = getModifiedNos();
		if (ids.isEmpty()) {
			return; // rien à faire
		}

		// au cas où il aurait été actif un moment dans la transaction...
		if (!isEnabled()) {
			ids.clear();
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Passage à dirty IDE des entreprises = " + Arrays.toString(ids.toArray()));
		}

		final TransactionTemplate template = new TransactionTemplate((PlatformTransactionManager) transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Switchable interceptor = (Switchable) sessionFactory.getSessionFactoryOptions().getInterceptor();
				final boolean enabled = interceptor.isEnabled();
				interceptor.setEnabled(false);
				try {
					final Session session = sessionFactory.openSession();
					try {
						final SQLQuery query = session.createSQLQuery("update TIERS set IDE_DIRTY = " + dialect.toBooleanValueString(true) + " where NUMERO in (:ids)");

						final BatchIterator<Long> batchIterator = new StandardBatchIterator<>(ids, 500);    // n'oublions pas qu'Oracle ne supporte pas plus de 1000 objets dans un IN
						while (batchIterator.hasNext()) {
							final Collection<Long> subSet = batchIterator.next();
							if (subSet != null && !subSet.isEmpty()) {
								query.setParameterList("ids", subSet);
								query.executeUpdate();
							}
						}

						session.flush();
					}
					finally {
						session.close();
					}
				}
				finally {
					interceptor.setEnabled(enabled);
				}
				return null;
			}
		});

		ids.clear();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Passage à dirty IDE des entreprises terminé.");
		}
	}

	@Override
	public void postTransactionRollback() {
		getModifiedNos().clear();
	}

	@Override
	public void setEnabled(boolean enabled) {
		activationSwitch.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return activationSwitch.isEnabled();
	}

	/**
	 * Ajoute l'entreprise spécifiée à la liste des cas qui seront marqué comme "dirty IDE" après le commit
	 * de la transaction principale.
	 * @param entreprise l'entreprise en question.
	 */
	private void addModifiedEntreprise(Entreprise entreprise) {
		if (entreprise != null) {
			addNoEntreprise(entreprise.getNumero());
		}
	}

	private void addNoEntreprise(long noEntreprise) {
		getModifiedNos().add(noEntreprise);
	}

	private Set<Long> getModifiedNos() {
		return modifiedNosEntreprises.get();
	}

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setServiceIDEService(ServiceIDEService serviceIDEService) {
		this.serviceIDEService = serviceIDEService;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}

package ch.vd.unireg.evenement.ide;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.StackedThreadLocal;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.common.ThreadSwitch;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.Tiers;

/**
 * Intercepteur de collecte des entreprises modifiées pour une éventuelle annonce de la modification à l'IDE
 */
public class AnnonceIDEHibernateInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean, Switchable {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDEHibernateInterceptor.class);

	private ModificationInterceptor parent;
	private SessionFactory sessionFactory;
	private PlatformTransactionManager transactionManager;
	private Dialect dialect;

	private final StackedThreadLocal<Set<Long>> modifiedNosEntreprises = new StackedThreadLocal<>(HashSet::new);

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
	public void suspendTransaction() {
		modifiedNosEntreprises.pushState();
	}

	@Override
	public void resumeTransaction() {
		modifiedNosEntreprises.popState();
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

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final boolean enabled = parent.isEnabledForThread();
		parent.setEnabledForThread(false);
		try {
			template.execute(status -> {
				final Session session = sessionFactory.openSession();
				try {
					final NativeQuery query = session.createNativeQuery("update TIERS set IDE_DIRTY = " + dialect.toBooleanValueString(true) + " where NUMERO in (:ids)");

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
				return null;
			});
		}
		finally {
			parent.setEnabledForThread(enabled);
		}

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

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}
}

package ch.vd.uniregctb.parentes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StackedThreadLocal;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityPhase;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * [SIFISC-9096] Cet intercepteur recalcule automatiquement les parentés sur les personnes physiques connues du civil et modifiées après le commit de chaque transaction.
 */
public class ParentesSynchronizerInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean, Switchable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParentesSynchronizerInterceptor.class);

	private ModificationInterceptor parent;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;

	private final Random randomGenerator = new Random();

	private final StackedThreadLocal<Set<Long>> modifiedNosIndividus = new StackedThreadLocal<>(HashSet::new);

	private final ThreadSwitch activationSwitch = new ThreadSwitch(true);

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		if (entity instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) entity;
			addModifiedPersonnePhysique(pp);
		}
		else if (entity instanceof LinkedEntity) {
			final LinkedEntity linkedEntity = (LinkedEntity) entity;
			final Set<Tiers> tiers = tiersService.getLinkedEntities(linkedEntity, Tiers.class, new LinkedEntityContext(LinkedEntityPhase.PARENTES, hibernateTemplate), isAnnulation);
			for (Tiers t : tiers) {
				if (t instanceof PersonnePhysique) {
					final PersonnePhysique pp = (PersonnePhysique) t;
					addModifiedPersonnePhysique(pp);
				}
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
		modifiedNosIndividus.pushState();
	}

	@Override
	public void resumeTransaction() {
		modifiedNosIndividus.popState();
	}

	@Override
	public void preTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionCommit() {

		final Set<Long> set = getModifiedNos();
		if (set.isEmpty()) {
			return; // rien à faire
		}

		// au cas où il aurait été actif un moment dans la transaction...
		if (!isEnabled()) {
			set.clear();
			return;
		}

		final boolean authenticated = AuthenticationHelper.hasCurrentPrincipal();

		// [UNIREG-2894] dans le context post-transactional suite à la réception d'un événement JMS, l'autentification n'est pas renseignée. On le fait donc à la volée ici.
		final String newPrincipal = authenticated ? String.format("%s-recalculParentes", AuthenticationHelper.getCurrentPrincipal()) : "AutoSynchroParentes";

		AuthenticationHelper.pushPrincipal(newPrincipal);
		try {
			parent.setEnabledForThread(false); // on désactive l'intercepteur pour éviter de s'intercepter soi-même
			activationSwitch.setEnabled(false);
			try {
				refreshParentes(set);
			}
			catch (OptimisticLockingFailureException e) {
				// [SIFISC-9599] en cas de problème de modification concurrente, on ré-essaie une fois
				try {
					final int waitingTime = 100 + Math.abs(randomGenerator.nextInt(500));    // attente entre 100 et 600 ms, aléatoire des fois qu'on serait plusieurs sur le coup
					LOGGER.warn(String.format("OptimisticLocking issue (%s): let's try again after a small break (%d ms)...", e.getMessage(), waitingTime));
					Thread.sleep(waitingTime);
					LOGGER.warn("OptimisticLocking issue: let's try again.");
					refreshParentes(set);
				}
				catch (RuntimeException | InterruptedException ex) {
					if (ex instanceof OptimisticLockingFailureException) {
						// encore ?!?!?
						// je disais donc, on ré-essaie une fois, et si cela ne fonctionne toujours pas, on
						// demande la levée du flag "parenté dirty" sur les personnes physiques correspondant à ces individus
						LOGGER.error("Renewed OptimisticLocking issue, the relationships will be marked dirty...", ex);
					}
					else {
						LOGGER.error("Error: the relationships will be marked dirty", ex);
					}

					markParentesDirty(set);
				}
			}
			catch (RuntimeException e) {
				LOGGER.error(e.getMessage(), e);
				markParentesDirty(set);
				throw e;
			}
			finally {
				parent.setEnabledForThread(true);
				activationSwitch.setEnabled(true);
				set.clear();
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void refreshParentes(final Set<Long> nosIndividus) {
		// on ré-ouvre une transaction pour effectuer les modifications nécessaires
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for (Long noIndividu : nosIndividus) {
					tiersService.refreshParentesDepuisNumeroIndividu(noIndividu);
				}
			}
		});
	}

	private void markParentesDirty(final Set<Long> nosIndividus) {
		// nouvelle transaction
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for (Long noIndividu : nosIndividus) {
					tiersService.markParentesDirtyDepuisNumeroIndividu(noIndividu);
				}
			}
		});
	}

	private void doInNewTransaction(TransactionCallbackWithoutResult callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.execute(callback);
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
	 * Ajoute la personne physique spécifiée dans la liste des cas qui seront rafraîchis après le flush.
	 * @param pp la personne physique en question.
	 */
	private void addModifiedPersonnePhysique(PersonnePhysique pp) {
		if (pp != null && pp.isConnuAuCivil()) {
			addNoIndividu(pp.getNumeroIndividu());
		}
	}

	/**
	 * Indique au processus que cet individu doit être rafraîchi de toute façon (i.e. même si la personne physique correspondante n'est pas modifiée).
	 * <p/>
	 * <b>Note :</b> cette méthode doit être appelée, si nécessaire, <i>avant</i> le {@link #postTransactionCommit()}
	 * @param noIndividu identifiant de l'individu cible
	 */
	public void forceRefreshOnIndividu(long noIndividu) {
		addNoIndividu(noIndividu);
	}

	private void addNoIndividu(long noIndividu) {
		getModifiedNos().add(noIndividu);
	}

	private Set<Long> getModifiedNos() {
		return modifiedNosIndividus.get();
	}

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
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

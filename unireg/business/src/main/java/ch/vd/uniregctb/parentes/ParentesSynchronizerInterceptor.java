package ch.vd.uniregctb.parentes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * [SIFISC-9096] Cet intercepteur recalcule automatiquement les parentés sur les personnes physiques connues du civil et modifiées après le commit de chaque transaction.
 */
public class ParentesSynchronizerInterceptor implements ModificationSubInterceptor, InitializingBean, Switchable {

	private static final Logger LOGGER = Logger.getLogger(ParentesSynchronizerInterceptor.class);

	private ModificationInterceptor parent;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;

	private final ThreadLocal<HashSet<Long>> modifiedNosIndividus = new ThreadLocal<HashSet<Long>>() {
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

		if (entity instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) entity;
			addModifiedPersonnePhysique(pp);
		}
		else if (entity instanceof LinkedEntity) {
			final LinkedEntity linkedEntity = (LinkedEntity) entity;
			final Set<Tiers> tiers = tiersService.getLinkedTiers(linkedEntity, isAnnulation);
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

		final boolean authenticated = AuthenticationHelper.isAuthenticated();

		// [UNIREG-2894] dans le context post-transactional suite à la réception d'un événement JMS, l'autentification n'est pas renseignée. On le fait donc à la volée ici.
		final String newPrincipal = authenticated ? String.format("%s-recalculParentes", AuthenticationHelper.getCurrentPrincipal()) : "AutoSynchroParentes";

		AuthenticationHelper.pushPrincipal(newPrincipal);
		try {
			parent.setEnabledForThread(false); // on désactive l'intercepteur pour éviter de s'intercepter soi-même
			activationSwitch.setEnabled(false);
			try {
				// on ré-ouvre une transaction pour effectuer les modifications nécessaires
				final TransactionTemplate template = new TransactionTemplate(transactionManager);
				template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				template.execute(new TransactionCallback<Object>() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						for (Long noIndividu : getModifiedNos()) {
							tiersService.refreshParentesDepuisNumeroIndividu(noIndividu);
						}
						return null;
					}
				});
			}
			catch (RuntimeException e) {
				LOGGER.error(e, e);
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

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}

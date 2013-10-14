package ch.vd.uniregctb.tache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.OtherPrincipalSynchronousExecutor;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * [UNIREG-2305] Cet interceptor recalcul automatiquement les tâches d'envoi et d'annulation de DIs sur les contribuables modifiées après le commit de chaque transaction.
 */
public class TacheSynchronizerInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean, Switchable {

	private static final Logger LOGGER = Logger.getLogger(TacheSynchronizerInterceptor.class);

	private ModificationInterceptor parent;
	private TacheService tacheService;
	private TiersService tiersService;
	private OtherPrincipalSynchronousExecutor syncExecutor;

	private final ThreadLocal<HashSet<Long>> modifiedCtbIds = new ThreadLocal<HashSet<Long>>() {
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

		if (entity instanceof Contribuable) {
			final Contribuable ctb = (Contribuable) entity;
			addModifiedContribuable(ctb);
		}
		else if (entity instanceof LinkedEntity) {
			final LinkedEntity linkedEntity = (LinkedEntity) entity;
			final Set<Tiers> tiers = tiersService.getLinkedTiers(linkedEntity, isAnnulation);
			for (Tiers t : tiers) {
				if (t instanceof Contribuable) {
					final Contribuable ctb = (Contribuable) t;
					addModifiedContribuable(ctb);
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

		final HashSet<Long> set = getModifiedCtbIds();
		if (set.isEmpty()) {
			return; // rien à faire
		}

		// [UNIREG-2894] dans le context post-transactional suite à la réception d'un événement JMS, l'autentification n'est pas renseignée. On le fait donc à la volée ici.
		final String newPrincipal = AuthenticationHelper.isAuthenticated() ? String.format("%s-recalculTaches", AuthenticationHelper.getCurrentPrincipal()) : "AutoSynchroTaches";

		// [SIFISC-9983] déportation du traitement dans un thread séparé afin que le visa trafiqué ne se risque pas de se retrouver dans la session HTTP...
		syncExecutor.doWithPrincipal(newPrincipal, new Runnable() {
			@Override
			public void run() {
				parent.setEnabledForThread(false); // on désactive l'intercepteur pour éviter de s'intercepter soi-même
				activationSwitch.setEnabled(false);
				try {
					tacheService.synchronizeTachesDIs(set);
					tacheService.annuleTachesObsoletes(set);
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
		});
	}

	@Override
	public void postTransactionRollback() {
		final HashSet<Long> set = getModifiedCtbIds();
		set.clear();
	}

	public void setOnTheFlySynchronization(boolean value) {
		setEnabled(value);
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
	 * Ajoute le contribuable spécifié dans les liste des contribuables qui seront indéxés après le flush.
	 *
	 * @param ctb le contribuable en question.
	 */
	private void addModifiedContribuable(Contribuable ctb) {
		if (ctb != null) {
			getModifiedCtbIds().add(ctb.getNumero());
		}
	}

	private HashSet<Long> getModifiedCtbIds() {
		return modifiedCtbIds.get();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
		syncExecutor = new OtherPrincipalSynchronousExecutor("TacheSync", 2, 16);
	}

	@Override
	public void destroy() throws Exception {
		syncExecutor.shutdown();
	}
}

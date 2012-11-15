package ch.vd.uniregctb.tiers.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class AutorisationCacheImpl implements AutorisationCache, DataEventListener, InitializingBean {

	private static class AutorisationKey {
		private final String visa;
		private final int oid;
		@Nullable
		private final Long tiersId;

		public AutorisationKey(@Nullable Long tiersId, String visa, int oid) {
			this.visa = visa;
			this.oid = oid;
			this.tiersId = tiersId;
		}

		@Nullable
		public Long getTiersId() {
			return tiersId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final AutorisationKey that = (AutorisationKey) o;

			if (oid != that.oid) return false;
			//noinspection SimplifiableIfStatement
			if (tiersId != null ? !tiersId.equals(that.tiersId) : that.tiersId != null) return false;
			return visa.equals(that.visa);
		}

		@Override
		public int hashCode() {
			int result = visa.hashCode();
			result = 31 * result + oid;
			result = 31 * result + (tiersId != null ? tiersId.hashCode() : 0);
			return result;
		}
	}

	private TiersDAO tiersDAO;
	private AutorisationManager autorisationManager;
	private PlatformTransactionManager transactionManager;
	private DataEventService dataEventService;

	private final Map<AutorisationKey, Autorisations> cache = Collections.synchronizedMap(new HashMap<AutorisationKey, Autorisations>());

	@Override
	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Override
	@NotNull
	public Autorisations getAutorisations(Long tiersId, String visa, int oid) {

		final AutorisationKey key = new AutorisationKey(tiersId, visa, oid);

		Autorisations auth = cache.get(key);
		if (auth == null) {
			auth = loadAutorisations(tiersId, visa, oid);
			cache.put(key, auth);
		}
		return auth;
	}

	private Autorisations loadAutorisations(final Long tiersId, final String visa, final int oid) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<Autorisations>() {
			@Override
			public Autorisations execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersId == null ? null : tiersDAO.get(tiersId);
				return autorisationManager.getAutorisations(tiers, visa, oid);
			}
		});
	}

	/**
	 * Supprime toutes les données cachées associées au tiers spécifié.
	 *
	 * @param id l'id du tiers dont les données cachées doivent être supprimées.
	 */
	private void evictTiers(long id) {
		synchronized (cache) {
			Iterator<Map.Entry<AutorisationKey, Autorisations>> iter = cache.entrySet().iterator();
			while (iter.hasNext()) {
				final Map.Entry<AutorisationKey, Autorisations> entry = iter.next();
				final Long tiersId = entry.getKey().getTiersId();
				if (tiersId != null && tiersId.equals(id)) {
					iter.remove();
				}
			}
		}
	}

	@Override
	public void onTiersChange(long id) {
		evictTiers(id);
	}

	@Override
	public void onIndividuChange(long id) {
		// rien à faire
	}

	@Override
	public void onPersonneMoraleChange(long id) {
		// rien à faire
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		evictTiers(tiersId);
	}

	@Override
	public void onTruncateDatabase() {
		synchronized (cache) {
			cache.clear();
		}
	}

	@Override
	public void onLoadDatabase() {
		synchronized (cache) {
			cache.clear();
		}
	}
}

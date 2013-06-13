package ch.vd.uniregctb.tiers.manager;

import java.io.Serializable;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.CacheHelper;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.DumpableUniregCache;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class AutorisationCacheImpl implements AutorisationCache, DataEventListener, InitializingBean, DumpableUniregCache {

	private static final Logger LOGGER = Logger.getLogger(AutorisationCacheImpl.class);

	private static class AutorisationKey implements Serializable {

		private static final long serialVersionUID = -760486881677996440L;

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

		@Override
		public String toString() {
			return "AutorisationKey{" +
					"visa='" + visa + '\'' +
					", oid=" + oid +
					", tiersId=" + tiersId +
					'}';
		}
	}

	private TiersDAO tiersDAO;
	private AutorisationManager autorisationManager;
	private PlatformTransactionManager transactionManager;
	private DataEventService dataEventService;
	private Cache cache;
	private String cacheName;
	private CacheManager cacheManager;
	private UniregCacheManager uniregCacheManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
		uniregCacheManager.register(this);
		cache = cacheManager.getCache(cacheName);
		Assert.notNull(cache);
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

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	@Override
	@NotNull
	public Autorisations getAutorisations(Long tiersId, String visa, int oid) {
		final Autorisations auth;

		final AutorisationKey key = new AutorisationKey(tiersId, visa, oid);
		final Element element = cache.get(key);
		if (element == null) {
			auth = loadAutorisations(tiersId, visa, oid);
			cache.put(new Element(key, auth));
		}
		else {
			auth = (Autorisations) element.getObjectValue();
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
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Eviction des autorisations cachées pour le tiers n° " + id);
		}
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof AutorisationKey) {
				final Long tiersId = ((AutorisationKey) k).getTiersId();
				remove = (tiersId != null && tiersId.equals(id));
			}
			if (remove) {
				cache.remove(k);
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
		reset();
	}

	@Override
	public void onLoadDatabase() {
		reset();
	}

	@Override
	public String getName() {
		return "AUTH-WEB";
	}

	@Override
	public String getDescription() {
		return "Cache des autorisations d'édition des tiers";
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	@Override
	public void reset() {
		cache.removeAll();
	}

	@Override
	public String dumpCacheKeys() {
		@SuppressWarnings("unchecked") final List<Object> keys = cache.getKeys();
		return CacheHelper.dumpKeys(keys);
	}
}

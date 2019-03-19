package ch.vd.unireg.interfaces.refsec;

import java.util.Objects;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.wsclient.refsec.RefSecClient;
import ch.vd.unireg.wsclient.refsec.RefSecClientException;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;

public class RefSecClientCache implements RefSecClient, UniregCacheInterface, InitializingBean, DisposableBean {

	private CacheManager cacheManager;
	private String cacheName;
	private Ehcache cache;
	private RefSecClient target;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	@Override
	public String getDescription() {
		return "client RefSec";
	}

	@Override
	public String getName() {
		return "REFSEC";
	}

	@Override
	public void reset() {
		cache.removeAll();
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("Le cache avec le nom [" + cacheName + "] est inconnu.");
		}
		if (statsService != null) {
			statsService.registerCache(ServiceInfrastructureRaw.SERVICE_NAME, this);
		}
		uniregCacheManager.register(this);
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(ServiceInfrastructureRaw.SERVICE_NAME);
		}
		uniregCacheManager.unregister(this);
	}

	private static class KeyGetProfilOperateur {
		@NotNull
		private final String visa;
		private final int collectivite;

		public KeyGetProfilOperateur(@NotNull String visa, int collectivite) {
			this.visa = visa;
			this.collectivite = collectivite;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof KeyGetProfilOperateur)) return false;
			final KeyGetProfilOperateur that = (KeyGetProfilOperateur) o;
			return collectivite == that.collectivite &&
					visa.equals(that.visa);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visa, collectivite);
		}
	}

	@Override
	@Nullable
	public ProfilOperateur getProfilOperateur(@NotNull String visa, int collectivite) throws RefSecClientException {
		final ProfilOperateur resultat;

		final KeyGetProfilOperateur key = new KeyGetProfilOperateur(visa, collectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getProfilOperateur(visa, collectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (ProfilOperateur) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCollectivitesOperateur {
		@NotNull
		private final String visa;

		private KeyGetCollectivitesOperateur(@NotNull String visa) {
			this.visa = visa;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof KeyGetCollectivitesOperateur)) return false;
			final KeyGetCollectivitesOperateur that = (KeyGetCollectivitesOperateur) o;
			return visa.equals(that.visa);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visa);
		}
	}

	@Override
	public Set<Integer> getCollectivitesOperateur(@NotNull String visa) throws RefSecClientException {
		final Set<Integer> resultat;

		final KeyGetCollectivitesOperateur key = new KeyGetCollectivitesOperateur(visa);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesOperateur(visa);
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (Set<Integer>) element.getObjectValue();
		}

		return resultat;
	}

	@Override
	public void ping() throws RefSecClientException {
		// on ne cache bien-sûr pas cet appel...
		target.ping();
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public void setTarget(RefSecClient target) {
		this.target = target;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}
}

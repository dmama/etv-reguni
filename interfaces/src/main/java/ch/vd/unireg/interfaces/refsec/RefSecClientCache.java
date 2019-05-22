package ch.vd.unireg.interfaces.refsec;

import java.util.List;
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
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.wsclient.refsec.RefSecClient;
import ch.vd.unireg.wsclient.refsec.RefSecClientException;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;
import ch.vd.unireg.wsclient.refsec.model.User;

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
			statsService.registerCache(InfrastructureConnector.SERVICE_NAME, this);
		}
		uniregCacheManager.register(this);
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(InfrastructureConnector.SERVICE_NAME);
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

	private static class KeyGetUser {
		@NotNull
		private final String visa;

		public KeyGetUser(@NotNull String visa) {
			this.visa = visa;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof KeyGetUser)) return false;
			final KeyGetUser that = (KeyGetUser) o;
			return visa.equals(that.visa);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visa);
		}
	}

	@Nullable
	@Override
	public User getUser(@NotNull String visa) throws RefSecClientException {
		final User resultat;
		final KeyGetUser key = new KeyGetUser(visa);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getUser(visa);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (User) element.getObjectValue();
		}
		return resultat;
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

	private static class KeyGetUsersFromCollectivite {
		@NotNull
		private final Integer collectivite;

		private KeyGetUsersFromCollectivite(@NotNull Integer collectivite) {
			this.collectivite = collectivite;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof KeyGetUsersFromCollectivite)) return false;
			final KeyGetUsersFromCollectivite that = (KeyGetUsersFromCollectivite) o;
			return collectivite.equals(that.collectivite);
		}

		@Override
		public int hashCode() {
			return Objects.hash(collectivite);
		}
	}

	@Override
	public List<User> getUsersFromCollectivite(@NotNull Integer collectivite) throws RefSecClientException {
		final List<User> resultat;

		final KeyGetUsersFromCollectivite key = new KeyGetUsersFromCollectivite(collectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getUsersFromCollectivite(collectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (List<User>) element.getObjectValue();
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

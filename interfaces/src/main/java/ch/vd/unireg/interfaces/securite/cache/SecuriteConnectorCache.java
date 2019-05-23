package ch.vd.unireg.interfaces.securite.cache;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.interfaces.civil.cache.IndividuConnectorCache;
import ch.vd.unireg.interfaces.securite.SecuriteConnector;
import ch.vd.unireg.interfaces.securite.SecuriteConnectorException;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.stats.StatsService;

public class SecuriteConnectorCache implements SecuriteConnector, UniregCacheInterface, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividuConnectorCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private SecuriteConnector target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			if (cache == null) {
				throw new IllegalArgumentException("Le cache avec le nom [" + cacheName + "] est inconnu.");
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initCache();
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
		if (uniregCacheManager != null) {
			uniregCacheManager.register(this);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		if (uniregCacheManager != null) {
			uniregCacheManager.unregister(this);
		}
	}

	@Override
	public String getDescription() {
		return "connecteur de sécurité";
	}

	@Override
	public String getName() {
		return "SECURITE";
	}

	@Override
	public void reset() {
		cache.removeAll();
	}

	private static class GetOperateurKey {
		@NotNull
		private final String visa;

		public GetOperateurKey(@NotNull String visa) {
			this.visa = visa;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GetOperateurKey)) return false;
			final GetOperateurKey that = (GetOperateurKey) o;
			return visa.equals(that.visa);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visa);
		}
	}

	@Nullable
	@Override
	public Operateur getOperateur(@NotNull String visa) throws SecuriteConnectorException {
		final Operateur resultat;

		final GetOperateurKey key = new GetOperateurKey(visa);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOperateur(visa);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Operateur) element.getObjectValue();
		}

		return resultat;
	}

	private static class GetProfileUtilisateurKey {
		@NotNull
		private final String visaOperateur;
		private final int codeCollectivite;

		public GetProfileUtilisateurKey(@NotNull String visaOperateur, int codeCollectivite) {
			this.visaOperateur = visaOperateur;
			this.codeCollectivite = codeCollectivite;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GetProfileUtilisateurKey)) return false;
			final GetProfileUtilisateurKey that = (GetProfileUtilisateurKey) o;
			return codeCollectivite == that.codeCollectivite &&
					visaOperateur.equals(that.visaOperateur);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visaOperateur, codeCollectivite);
		}
	}

	@Nullable
	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws SecuriteConnectorException {
		final ProfileOperateur resultat;

		final GetProfileUtilisateurKey key = new GetProfileUtilisateurKey(visaOperateur, codeCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getProfileUtilisateur(visaOperateur, codeCollectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (ProfileOperateur) element.getObjectValue();
		}

		return resultat;
	}


	private static class GetUtilisateursKey {
		private final int noCollAdmin;

		public GetUtilisateursKey(int noCollAdmin) {
			this.noCollAdmin = noCollAdmin;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GetUtilisateursKey)) return false;
			final GetUtilisateursKey that = (GetUtilisateursKey) o;
			return noCollAdmin == that.noCollAdmin;
		}

		@Override
		public int hashCode() {
			return Objects.hash(noCollAdmin);
		}
	}

	@Override
	public @NotNull List<String> getUtilisateurs(int noCollAdmin) throws SecuriteConnectorException {
		final List<String> resultat;

		final GetUtilisateursKey key = new GetUtilisateursKey(noCollAdmin);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getUtilisateurs(noCollAdmin);
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (List<String>) element.getObjectValue();
		}

		return resultat;
	}

	private static class GetCollectivitesOperateurKey {
		@NotNull
		private final String visaOperateur;

		public GetCollectivitesOperateurKey(@NotNull String visaOperateur) {
			this.visaOperateur = visaOperateur;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GetCollectivitesOperateurKey)) return false;
			final GetCollectivitesOperateurKey that = (GetCollectivitesOperateurKey) o;
			return visaOperateur.equals(that.visaOperateur);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visaOperateur);
		}
	}

	@NotNull
	@Override
	public Set<Integer> getCollectivitesOperateur(@NotNull String visaOperateur) throws SecuriteConnectorException {
		final Set<Integer> resultat;

		final GetCollectivitesOperateurKey key = new GetCollectivitesOperateurKey(visaOperateur);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesOperateur(visaOperateur);
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (Set<Integer>) element.getObjectValue();
		}

		return resultat;
	}

	@Override
	public void ping() throws SecuriteConnectorException {
		target.ping();
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public void setTarget(SecuriteConnector target) {
		this.target = target;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}
}

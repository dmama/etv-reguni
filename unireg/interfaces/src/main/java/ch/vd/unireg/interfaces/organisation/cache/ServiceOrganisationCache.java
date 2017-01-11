package ch.vd.unireg.interfaces.organisation.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationServiceWrapper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.uniregctb.cache.CacheHelper;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.KeyDumpableCache;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.data.SourceDataEventListener;
import ch.vd.uniregctb.data.SourceDataEventService;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.utils.LogLevel;

public class ServiceOrganisationCache implements ServiceOrganisationRaw, UniregCacheInterface, KeyDumpableCache, SourceDataEventListener, InitializingBean, DisposableBean, ServiceOrganisationServiceWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOrganisationCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private ServiceOrganisationRaw target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private SourceDataEventService dataEventService;

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	public void setCacheManager(CacheManager manager) {
		this.cacheManager = manager;
		initCache();
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
		initCache();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(SourceDataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
		if (uniregCacheManager != null) {
			uniregCacheManager.register(this);
		}
		dataEventService.register(this);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "service organisation";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "ORGANISATION";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.removeAll();
	}

	@Override
	public ServiceOrganisationRaw getTarget() {
		return target;
	}

	@Override
	public ServiceOrganisationRaw getUltimateTarget() {
		if (target instanceof ServiceOrganisationServiceWrapper) {
			return ((ServiceOrganisationServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}

	private static class GetOrganisationKey implements Serializable {

		private static final long serialVersionUID = 3198014557405952141L;

		private final long noOrganisation;

		private GetOrganisationKey(long noOrganisation) {
			this.noOrganisation = noOrganisation;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetOrganisationKey that = (GetOrganisationKey) o;
			return noOrganisation == that.noOrganisation;

		}

		@Override
		public int hashCode() {
			return (int) (noOrganisation ^ (noOrganisation >>> 32));
		}

		@Override
		public String toString() {
			return "GetOrganisationKey{" +
					"noOrganisation=" + noOrganisation +
					'}';
		}
	}

	private static class GetSiteOrganisationKey implements Serializable {

		private static final long serialVersionUID = 3198014557405952141L;

		private final long noSite;

		private GetSiteOrganisationKey(long noSite) {
			this.noSite = noSite;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetSiteOrganisationKey that = (GetSiteOrganisationKey) o;

			return noSite == that.noSite;

		}

		@Override
		public int hashCode() {
			return (int) (noSite ^ (noSite >>> 32));
		}

		@Override
		public String toString() {
			return "GetSiteOrganisationKey{" +
					"noSite=" + noSite +
					'}';
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Organisation getOrganisationHistory(final long noOrganisation) throws ServiceOrganisationException {

		final GetOrganisationKey key = new GetOrganisationKey(noOrganisation);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			final Organisation organisation = target.getOrganisationHistory(noOrganisation);
			Objects.requireNonNull(organisation);
			cache.put(new Element(key, organisation));
			return organisation;
		}
		return (Organisation) element.getObjectValue();
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		final GetSiteOrganisationKey key = new GetSiteOrganisationKey(noSite);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			final Long noSiteRecupere = target.getOrganisationPourSite(noSite);
			Objects.requireNonNull(noSiteRecupere);
			cache.put(new Element(key, noSiteRecupere));
			return noSiteRecupere;
		}
		return (Long) element.getObjectValue();
	}

	private static class GetOrganisationByNoIdeKey implements Serializable {

		private static final long serialVersionUID = 630591634651995670L;

		private final String noide;

		public GetOrganisationByNoIdeKey(String noide) {
			this.noide = noide;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetOrganisationByNoIdeKey that = (GetOrganisationByNoIdeKey) o;
			return noide != null ? noide.equals(that.noide) : that.noide == null;
		}

		@Override
		public int hashCode() {
			return noide != null ? noide.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "GetOrganisationByNoIdeKey{" +
					"noide='" + noide + '\'' +
					'}';
		}
	}

	@Override
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		final GetOrganisationByNoIdeKey key = new GetOrganisationByNoIdeKey(noide);
		final Element element = cache.get(key);
		if (element == null) {
			final Identifiers ids = target.getOrganisationByNoIde(noide);
			cache.put(new Element(key, ids));
			return ids;
		}
		else {
			return (Identifiers) element.getValue();
		}
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		return target.getOrganisationEvent(noEvenement);
	}

	@Override
	public AnnonceIDE getAnnonceIDE(long numero) {
		return target.getAnnonceIDE(numero);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {
		// pas de cache sur les recherches
		return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) {
		return target.validerAnnonceIDE(modele);
	}

	@Override
	public void ping() throws ServiceCivilException {
		target.ping();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onIndividuChange(long numero) {
		// rien à faire
	}

	@Override
	public void onOrganisationChange(long id) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Eviction des données cachées pour l'organisation n° " + id);
		}
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof GetOrganisationKey) {
				final GetOrganisationKey ko = (GetOrganisationKey) k;
				remove = (ko.noOrganisation == id);
			}
			else if (k instanceof GetSiteOrganisationKey) {
				final GetSiteOrganisationKey ks = (GetSiteOrganisationKey) k;
				remove = (ks.noSite == id);
				if (!remove) {
					final Element elt = cache.getQuiet(k);
					final Object value = elt != null ? elt.getObjectValue() : null;
					if (value != null) {
						remove = value.equals(id);
					}
				}
			}
			if (remove) {
				cache.remove(k);
			}
		}
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}
}

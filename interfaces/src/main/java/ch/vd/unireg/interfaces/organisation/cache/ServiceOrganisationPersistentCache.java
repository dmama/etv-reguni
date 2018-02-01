package ch.vd.unireg.interfaces.organisation.cache;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationServiceWrapper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.ObjectKey;
import ch.vd.unireg.cache.PersistentCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.stats.StatsService;

public class ServiceOrganisationPersistentCache implements ServiceOrganisationRaw, UniregCacheInterface, CivilDataEventListener, InitializingBean, DisposableBean, ServiceOrganisationServiceWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOrganisationPersistentCache.class);

	public static final String CACHE_NAME = "ServiceOrganisationPersistent";

	private PersistentCache<OrganisationDataCache> cache;
	private PersistentCache<Long> siteCache;
	private ServiceOrganisationRaw target;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private CivilDataEventService dataEventService;

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCache(PersistentCache<OrganisationDataCache> cache) {
		this.cache = cache;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSiteCache(PersistentCache<Long> siteCache) {
		this.siteCache = siteCache;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(CivilDataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Override
	public CacheStats buildStats() {
		return cache.buildStats();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(CACHE_NAME, this);
		}
		uniregCacheManager.register(this);
		dataEventService.register(this);
	}

	@Override
	public void destroy() throws Exception {
		uniregCacheManager.unregister(this);
		if (statsService != null) {
			statsService.unregisterCache(CACHE_NAME);
		}
		dataEventService.unregister(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "service organisation persistent";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "ORGANISATION-PERSISTENT";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.clear();
		siteCache.clear();
	}

	private static class GetOrganisationHistoryKey implements ObjectKey {

		private static final long serialVersionUID = -1985928878473056014L;

		private final long noOrganisation;

		private GetOrganisationHistoryKey(long noOrganisation) {
			this.noOrganisation = noOrganisation;
		}

		@Override
		public long getId() {
			return noOrganisation;
		}

		@Override
		public String getComplement() {
			return "";
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {

		final GetOrganisationHistoryKey key = new GetOrganisationHistoryKey(noOrganisation);
		final OrganisationDataCache value = cache.get(key);
		if (value == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			final Organisation organisation = target.getOrganisationHistory(noOrganisation);
			Objects.requireNonNull(organisation, String.format("Aucune organisation retournée par le service pour le no: %s", noOrganisation));

			cache.put(key, new OrganisationDataCache(organisation));
			return organisation;
		}
		return value.getOrganisation();
	}

	private static class GetOrganisationForSiteKey implements ObjectKey {

		private static final long serialVersionUID = 5394436700031462099L;

		private final long noSite;

		private GetOrganisationForSiteKey(long noSite) {
			this.noSite = noSite;
		}

		@Override
		public long getId() {
			return noSite;
		}

		@Override
		public String getComplement() {
			return "";
		}
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {

		Long noOrganisation;

		final GetOrganisationForSiteKey key = new GetOrganisationForSiteKey(noSite);
		final Long value = siteCache.get(key);
		if (value == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			noOrganisation = target.getOrganisationPourSite(noSite);
			Objects.requireNonNull(noOrganisation, String.format("Aucun numéro d'organisation retourné par le service pour le no de site: %s", noSite));
			siteCache.put(key, noOrganisation);

			return noOrganisation;
		}
		return value;
	}

	@Override
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		// pas caché pour le moment...
		return target.getOrganisationByNoIde(noide);
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		// aucun intérêt à cacher ce genre d'information
		return target.getOrganisationEvent(noEvenement);
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
	public void ping() throws ServiceOrganisationException {
		target.ping();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onOrganisationChange(final long numero) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Eviction des données cachées pour l'organisation n° " + numero);
		}
		cache.removeAll(numero);
		siteCache.removeAll(numero);
		siteCache.removeValues(new Predicate<Long>() {
			@Override
			public boolean evaluate(Long object) {
				return numero == object;
			}
		});
	}

	@Override
	public void onIndividuChange(long numero) {
		// rien à faire
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
}

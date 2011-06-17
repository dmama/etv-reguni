package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.tiers3.BatchTiers;
import ch.vd.unireg.webservices.tiers3.BatchTiersEntry;
import ch.vd.unireg.webservices.tiers3.DebiteurInfo;
import ch.vd.unireg.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.unireg.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.unireg.webservices.tiers3.GetListeCtbModifiesRequest;
import ch.vd.unireg.webservices.tiers3.GetTiersRequest;
import ch.vd.unireg.webservices.tiers3.GetTiersTypeRequest;
import ch.vd.unireg.webservices.tiers3.QuittancerDeclarationsRequest;
import ch.vd.unireg.webservices.tiers3.QuittancerDeclarationsResponse;
import ch.vd.unireg.webservices.tiers3.SearchEvenementsPMRequest;
import ch.vd.unireg.webservices.tiers3.SearchEvenementsPMResponse;
import ch.vd.unireg.webservices.tiers3.SearchTiersRequest;
import ch.vd.unireg.webservices.tiers3.SearchTiersResponse;
import ch.vd.unireg.webservices.tiers3.SetTiersBlocRembAutoRequest;
import ch.vd.unireg.webservices.tiers3.Tiers;
import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.unireg.webservices.tiers3.TiersWebService;
import ch.vd.unireg.webservices.tiers3.TypeTiers;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

public class TiersWebServiceCache implements UniregCacheInterface, TiersWebService, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "TiersWebService3";

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private TiersWebService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	public void setTarget(TiersWebService target) {
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	// pour le testing
	protected Ehcache getEhCache() {
		return cache;
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
		uniregCacheManager.register(this);
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		uniregCacheManager.unregister(this);
	}

	@Override
	public Tiers getTiers(GetTiersRequest params) throws WebServiceException {

		final Tiers tiers;

		final GetTiersKey key = new GetTiersKey(params.getTiersNumber());
		final HashSet<TiersPart> parts = (params.getParts() == null ? null : new HashSet<TiersPart>(params.getParts()));

		try {
			final Element element = cache.get(key);
			if (element == null) {
				tiers = target.getTiers(params);
				GetTiersValue value = new GetTiersValue(parts, tiers);
				cache.put(new Element(key, value));
			}
			else {
				GetTiersValue value = (GetTiersValue) element.getObjectValue();
				if (value.isNull()) {
					tiers = null;
				}
				else {
					Set<TiersPart> delta = value.getMissingParts(parts);
					if (delta != null) {
						// on complète la liste des parts à la volée
						final GetTiersRequest deltaRequest = new GetTiersRequest(params.getLogin(), params.getTiersNumber(), new ArrayList<TiersPart>(delta));
						Tiers deltaTiers = target.getTiers(deltaRequest);
						value.addParts(delta, deltaTiers);
					}
					tiers = value.getValueForParts(parts);
				}
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return tiers;
	}

	@Override
	public BatchTiers getBatchTiers(GetBatchTiersRequest params) throws WebServiceException {

		try {
			if (params.getTiersNumbers() == null || params.getTiersNumbers().isEmpty()) {
				return new BatchTiers();
			}

			final BatchTiers batch;

			// on récupère tout ce qu'on peut dans le cache
			final List<BatchTiersEntry> cachedEntries = getCachedBatchTiersHistoEntries(params);
			if (cachedEntries == null || cachedEntries.isEmpty()) {
				// rien trouvé -> on passe tout droit
				batch = target.getBatchTiers(params);

				// on met-à-jour le cache
				cacheBatchTiersEntries(batch, params);
			}
			else {
				// trouvé des données dans le cache -> on détermine ce qui manque
				final List<Long> uncachedIds = extractUncachedTiersHistoIds(params.getTiersNumbers(), cachedEntries);
				if (uncachedIds.isEmpty()) {
					batch = new BatchTiers();
				}
				else {
					// on demande plus loin ce qui manque
					batch = target.getBatchTiers(new GetBatchTiersRequest(params.getLogin(), uncachedIds, params.getParts()));

					// on met-à-jour le cache
					cacheBatchTiersEntries(batch, params);
				}

				// on mélange les deux collections
				batch.getEntries().addAll(cachedEntries);
			}

			return batch;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * Met-à-jour le cache à partir de données nouvellement extraites.
	 *
	 * @param batch  les données nouvellement extraites
	 * @param params les paramètres demandés correspondant aux données spécifiées.
	 */
	private void cacheBatchTiersEntries(BatchTiers batch, GetBatchTiersRequest params) {
		final Set<TiersPart> parts = (params.getParts() == null ? null : new HashSet<TiersPart>(params.getParts()));
		for (BatchTiersEntry entry : batch.getEntries()) {
			if (entry.getExceptionInfo() != null) {   // [UNIREG-3288] on ignore les tiers qui ont levé une exception
				continue;
			}

			final GetTiersKey key = new GetTiersKey(entry.getNumber());

			final Element element = cache.get(key);
			if (element == null) {
				// on enregistre le nouveau tiers dans le cache
				GetTiersValue value = new GetTiersValue(parts, entry.getTiers());
				cache.put(new Element(key, value));
			}
			else if (entry.getTiers() != null) {
				// on met-à-jour le tiers (s'il existe) avec les parts chargées
				GetTiersValue value = (GetTiersValue) element.getObjectValue();
				Assert.isFalse(value.isNull());
				value.addParts(parts, entry.getTiers());
			}
		}
	}

	/**
	 * Extrait l'ensemble des ids de tiers non trouvés dans le cache.
	 *
	 * @param requestedIds  l'ensemble des ids demandés
	 * @param cachedEntries les données trouvées dans le cache
	 * @return l'ensemble des ids non trouvés dans le cache.
	 */
	private List<Long> extractUncachedTiersHistoIds(List<Long> requestedIds, List<BatchTiersEntry> cachedEntries) {
		final Set<Long> cachedIds = new HashSet<Long>(cachedEntries.size());
		for (BatchTiersEntry entry : cachedEntries) {
			cachedIds.add(entry.getNumber());
		}
		final Set<Long> uncached = new HashSet<Long>(requestedIds.size() - cachedIds.size());
		for (Long id : requestedIds) {
			if (!cachedIds.contains(id)) {
				uncached.add(id);
			}
		}
		return new ArrayList<Long>(uncached);
	}

	/**
	 * Retourne les données disponibles dans le cache qui correspondent aux paramètres demandés. Si un tiers existe dans le cache mais que toutes les parts demandées ne sont pas renseignées, on
	 * l'ignore.
	 *
	 * @param params les paramètres de l'appel
	 * @return une liste des données trouvées dans le cache
	 */
	private List<BatchTiersEntry> getCachedBatchTiersHistoEntries(GetBatchTiersRequest params) {

		List<BatchTiersEntry> cachedEntries = null;

		final Set<TiersPart> parts = (params.getParts() == null ? null : new HashSet<TiersPart>(params.getParts()));

		for (Long id : params.getTiersNumbers()) {
			final GetTiersKey key = new GetTiersKey(id);

			final Element element = cache.get(key);
			if (element == null) {
				continue;
			}

			GetTiersValue value = (GetTiersValue) element.getObjectValue();
			if (value.isNull()) {
				continue;
			}

			Set<TiersPart> delta = value.getMissingParts(parts);
			if (delta != null) {
				continue;
			}

			if (cachedEntries == null) {
				cachedEntries = new ArrayList<BatchTiersEntry>();
			}
			final Tiers tiers = value.getValueForParts(parts);
			final BatchTiersEntry entry = new BatchTiersEntry(id, tiers, null);

			cachedEntries.add(entry);
		}

		return cachedEntries;
	}

	@Override
	public TypeTiers getTiersType(GetTiersTypeRequest params) throws WebServiceException {

		final TypeTiers resultat;

		try {
			final GetTiersTypeKey key = new GetTiersTypeKey(params.getTiersNumber());
			final Element element = cache.get(key);
			if (element == null) {
				resultat = target.getTiersType(params);
				cache.put(new Element(key, resultat));
			}
			else {
				resultat = (TypeTiers) element.getObjectValue();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return resultat;
	}

	@Override
	public SearchTiersResponse searchTiers(SearchTiersRequest params) throws WebServiceException {
		// pas de cache pour l'instant
		return target.searchTiers(params);
	}

	@Override
	public void setTiersBlocRembAuto(SetTiersBlocRembAutoRequest params) throws WebServiceException {
		// pas de valeur retournée -> rien à cacher
		target.setTiersBlocRembAuto(params);
	}

	@Override
	public SearchEvenementsPMResponse searchEvenementsPM(SearchEvenementsPMRequest params) throws WebServiceException {
		// on ne cache pas les événements PM car on ne sait pas quand ils changent
		return target.searchEvenementsPM(params);
	}

	@Override
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfoRequest params) throws WebServiceException {

		final DebiteurInfo resultat;

		final GetDebiteurInfoKey key = new GetDebiteurInfoKey(params.getNumeroDebiteur(), params.getPeriodeFiscale());

		try {
			final Element element = cache.get(key);
			if (element == null) {
				resultat = target.getDebiteurInfo(params);
				cache.put(new Element(key, resultat));
			}
			else {
				resultat = (DebiteurInfo) element.getObjectValue();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return resultat;
	}

	@Override
	public QuittancerDeclarationsResponse quittancerDeclarations(QuittancerDeclarationsRequest params) throws WebServiceException {
		return target.quittancerDeclarations(params);
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Long[] getListeCtbModifies(GetListeCtbModifiesRequest params) throws WebServiceException {
		//données mouvantes inutile de cacher
		return target.getListeCtbModifies(params);
	}

	/**
	 * Supprime tous les éléments cachés sur le tiers spécifié.
	 *
	 * @param id le numéro de tiers
	 */
	public void evictTiers(long id) {

		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof CacheKey) {
				CacheKey ck = (CacheKey) k;
				remove = (ck.tiersNumber == id);
			}
			if (remove) {
				cache.remove(k);
			}
		}
	}

	/**
	 * Efface complétement le cache.
	 */
	public void clearAll() {
		cache.removeAll();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "web-service tiers v3";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "WS-TIERS-3";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.removeAll();
	}
}

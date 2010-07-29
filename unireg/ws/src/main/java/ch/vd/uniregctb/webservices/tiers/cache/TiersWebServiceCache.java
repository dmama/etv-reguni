package ch.vd.uniregctb.webservices.tiers.cache;

import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.stats.StatsService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.webservices.common.WebServiceException;
import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.TiersHisto;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.TiersPart;
import ch.vd.uniregctb.webservices.tiers.TiersWebService;
import ch.vd.uniregctb.webservices.tiers.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers.params.SetTiersBlocRembAuto;

public class TiersWebServiceCache implements UniregCacheInterface, TiersWebService, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "TiersWebService";

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

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public Ehcache getEhCache() {
		return cache;
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, cache);
		}
		uniregCacheManager.register(this);
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		uniregCacheManager.unregister(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public Tiers getTiers(GetTiers params) throws WebServiceException {

		final Tiers tiers;

		final GetTiersKey key = new GetTiersKey(params.tiersNumber, params.date);

		try {
			final Element element = cache.get(key);
			if (element == null) {
				tiers = target.getTiers(params);
				GetTiersValue value = new GetTiersValue(params.parts, tiers);
				cache.put(new Element(key, value));
			}
			else {
				GetTiersValue value = (GetTiersValue) element.getObjectValue();
				if (value.isNull()) {
					tiers = null;
				}
				else {
					Set<TiersPart> delta = value.getMissingParts(params.parts);
					if (delta != null) {
						// on complète la liste des parts à la volée
						Tiers deltaTiers = target.getTiers(params.clone(delta));
						value.addParts(delta, deltaTiers);
					}
					tiers = value.getValueForParts(params.parts);
				}
			}
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
		}

		return tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public TiersHisto getTiersHisto(GetTiersHisto params) throws WebServiceException {

		final TiersHisto tiers;

		final GetTiersHistoKey key = new GetTiersHistoKey(params.tiersNumber);

		try {
			final Element element = cache.get(key);
			if (element == null) {
				tiers = target.getTiersHisto(params);
				GetTiersHistoValue value = new GetTiersHistoValue(params.parts, tiers);
				cache.put(new Element(key, value));
			}
			else {
				GetTiersHistoValue value = (GetTiersHistoValue) element.getObjectValue();
				if (value.isNull()) {
					tiers = null;
				}
				else {
					Set<TiersPart> delta = value.getMissingParts(params.parts);
					if (delta != null) {
						// on complète la liste des parts à la volée
						TiersHisto deltaTiers = target.getTiersHisto(params.clone(delta));
						value.addParts(delta, deltaTiers);
					}
					tiers = value.getValueForParts(params.parts);
				}
			}
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
		}

		return tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws WebServiceException {

		final TiersHisto tiers;

		final GetTiersPeriodeKey key = new GetTiersPeriodeKey(params.tiersNumber, params.periode);

		try {
			final Element element = cache.get(key);
			if (element == null) {
				tiers = target.getTiersPeriode(params);
				GetTiersHistoValue value = new GetTiersHistoValue(params.parts, tiers);
				cache.put(new Element(key, value));
			}
			else {
				GetTiersHistoValue value = (GetTiersHistoValue) element.getObjectValue();
				if (value.isNull()) {
					tiers = null;
				}
				else {
					Set<TiersPart> delta = value.getMissingParts(params.parts);
					if (delta != null) {
						// on complète la liste des parts à la volée
						TiersHisto deltaTiers = target.getTiersPeriode(params.clone(delta));
						value.addParts(delta, deltaTiers);
					}
					tiers = value.getValueForParts(params.parts);
				}
			}
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
		}

		return tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getTiersType(GetTiersType params) throws WebServiceException {

		final Type resultat;

		try {
			final Element element = cache.get(params);
			if (element == null) {
				resultat = target.getTiersType(params);
				cache.put(new Element(params, resultat));
			}
			else {
				resultat = (Type) element.getObjectValue();
			}
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TiersInfo> searchTiers(SearchTiers params) throws WebServiceException {

		final List<TiersInfo> resultat;

		try {
			final Element element = cache.get(params);
			if (element == null) {
				resultat = target.searchTiers(params);
				cache.put(new Element(params, resultat));
			}
			else {
				resultat = (List<TiersInfo>) element.getObjectValue();
			}
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws WebServiceException {
		// pas de valeur retournée -> rien à cacher
		target.setTiersBlocRembAuto(params);
	}

	/**
	 * {@inheritDoc}
	 */
	public void doNothing(AllConcreteTiersClasses dummy) {
	}

	/**
	 * Supprime tous les éléments cachés sur le tiers spécifié.
	 *
	 * @param id
	 *            le numéro de tiers
	 */
	public void evictTiers(long id) {

		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof CacheKey) {
				CacheKey ck = (CacheKey) k;
				remove = (ck.tiersNumber == id);
			}
			else if (k instanceof SearchTiers) {
				SearchTiers s = (SearchTiers) k;
				if (s.numero != null) {
					try {
						long n = Long.parseLong(s.numero);
						remove = (n == id);
					}
					catch (NumberFormatException ignored) {
						// tant pis, on ignore
					}
				}
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
	public String getDescription() {
		return "web-service tiers v1";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "WS-TIERS";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		cache.removeAll();
	}
}

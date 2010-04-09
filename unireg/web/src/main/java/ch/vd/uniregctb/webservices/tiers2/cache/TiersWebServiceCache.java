package ch.vd.uniregctb.webservices.tiers2.cache;

import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.tiers2.data.*;
import ch.vd.uniregctb.webservices.tiers2.params.*;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;

public class TiersWebServiceCache implements UniregCacheInterface, TiersWebService, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "TiersWebService2";

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

	/** for testing purposes only ! */
	protected Ehcache getEhCache() {
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
	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

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
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}

		return tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

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
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}

		return tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public BatchTiers getBatchTiers(GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

		// TODO (msi) Implémenter un cache intelligent pour les batches
		return target.getBatchTiers(params);
//		final BatchTiers batch = new BatchTiers();
//
//		final Map<Long, Tiers> tiersCaches;
//
//		// Note: pour les requêtes batch, on ne cache pas la requête complète : il n'y a pas assez de probabilités que la même requête soit effectuée avec exactement les mêmes numéro de tiers.
//
//		for (Long id : params.tiersNumbers) {
//
//			final GetTiersKey key = new GetTiersKey(id, params.date);
//			final Element element = cache.get(key);
//			if (element != null) {
//				final GetTiersValue value = (GetTiersValue) element.getObjectValue();
//				boolean complet = (value.getMissingParts(params.parts) == null);
//				Tiers tiers = value.getValueForParts(params.parts);
//			}
//
//			BatchTiersEntry entry;
//			try {
//				Tiers tiers = getTiers(subparam);
//				entry = new BatchTiersEntry(id, tiers);
//			}
//			catch (WebServiceException e) {
//				entry = new BatchTiersEntry(id, e);
//			}
//			catch (Exception e) {
//				entry = new BatchTiersEntry(id, new WebServiceException(e));
//			}
//
//			batch.entries.add(entry);
//		}
//
//		return batch;
	}

	/**
	 * {@inheritDoc}
	 */
	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		return target.getBatchTiersHisto(params);
	}

	/**
	 * {@inheritDoc}
	 */
	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {

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
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}

		return tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {

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
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

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
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		// pas de valeur retournée -> rien à cacher
		target.setTiersBlocRembAuto(params);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<EvenementPM> searchEvenementsPM(SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {
		// on ne cache pas les événements PM car on ne sait pas quand ils changent
		return target.searchEvenementsPM(params);
	}

	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws
			BusinessException, AccessDeniedException, TechnicalException {

		final DebiteurInfo resultat;

		try {
			final Element element = cache.get(params);
			if (element == null) {
				resultat = target.getDebiteurInfo(params);
				cache.put(new Element(params, resultat));
			}
			else {
				resultat = (DebiteurInfo) element.getObjectValue();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws BusinessException, AccessDeniedException, TechnicalException {
		// méthode de modification -> rien à cacher
		return target.quittancerDeclarations(params);
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
		return "web-service tiers v2";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "WS-TIERS-2";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		cache.removeAll();
	}
}

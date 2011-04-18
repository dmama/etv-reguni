package ch.vd.uniregctb.webservices.tiers2.cache;

import javax.jws.WebParam;
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
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetListeCtbModifies;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

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

	// pour le testing
	protected Ehcache getEhCache() {
		return cache;
	}

	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
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

		try {
			if (params.tiersNumbers == null || params.tiersNumbers.isEmpty()) {
				return new BatchTiers();
			}

			final BatchTiers batch;

			// on récupère tout ce qu'on peut dans le cache
			final List<BatchTiersEntry> cachedEntries = getCachedBatchTiersEntries(params);
			if (cachedEntries == null || cachedEntries.isEmpty()) {
				// rien trouvé -> on passe tout droit
				batch = target.getBatchTiers(params);

				// on met-à-jour le cache
				cacheBatchTiersEntries(batch, params);
			}
			else {
				// trouvé des données dans le cache -> on détermine ce qui manque
				final Set<Long> uncachedIds = extractUncachedTiersIds(params.tiersNumbers, cachedEntries);
				if (uncachedIds.isEmpty()) {
					batch = new BatchTiers();
				}
				else {
					// on demande plus loin ce qui manque
					batch = target.getBatchTiers(new GetBatchTiers(params.login, uncachedIds, params.date, params.parts));

					// on met-à-jour le cache
					cacheBatchTiersEntries(batch, params);
				}

				// on mélange les deux collections
				batch.entries.addAll(cachedEntries);
			}

			return batch;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * Met-à-jour le cache à partir de données nouvellement extraites.
	 *
	 * @param batch  les données nouvellement extraites
	 * @param params les paramètres demandés correspondant aux données spécifiées.
	 */
	private void cacheBatchTiersEntries(BatchTiers batch, GetBatchTiers params) {
		for (BatchTiersEntry entry : batch.entries) {
			if (entry.exceptionMessage != null) {   // [UNIREG-3288] on ignore les tiers qui ont levé une exception
				continue;
			}

			final GetTiersKey key = new GetTiersKey(entry.number, params.date);

			final Element element = cache.get(key);
			if (element == null) {
				// on enregistre le nouveau tiers dans le cache
				GetTiersValue value = new GetTiersValue(params.parts, entry.tiers);
				cache.put(new Element(key, value));
			}
			else if (entry.tiers != null) {
				// on met-à-jour le tiers (s'il existe) avec les parts chargées
				GetTiersValue value = (GetTiersValue) element.getObjectValue();
				Assert.isFalse(value.isNull());
				value.addParts(params.parts, entry.tiers);
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
	private Set<Long> extractUncachedTiersIds(Set<Long> requestedIds, List<BatchTiersEntry> cachedEntries) {
		final Set<Long> cachedIds = new HashSet<Long>(cachedEntries.size());
		for (BatchTiersEntry entry : cachedEntries) {
			cachedIds.add(entry.number);
		}
		final Set<Long> uncached = new HashSet<Long>(requestedIds.size() - cachedIds.size());
		for (Long id : requestedIds) {
			if (!cachedIds.contains(id)) {
				uncached.add(id);
			}
		}
		return uncached;
	}

	/**
	 * Retourne les données disponibles dans le cache qui correspondent aux paramètres demandés. Si un tiers existe dans le cache mais que toutes les parts demandées ne sont pas renseignées, on
	 * l'ignore.
	 *
	 * @param params les paramètres de l'appel
	 * @return une liste des données trouvées dans le cache
	 */
	private List<BatchTiersEntry> getCachedBatchTiersEntries(GetBatchTiers params) {

		List<BatchTiersEntry> cachedEntries = null;

		for (Long id : params.tiersNumbers) {
			final GetTiersKey key = new GetTiersKey(id, params.date);

			final Element element = cache.get(key);
			if (element == null) {
				continue;
			}

			GetTiersValue value = (GetTiersValue) element.getObjectValue();
			if (value.isNull()) {
				continue;
			}

			Set<TiersPart> delta = value.getMissingParts(params.parts);
			if (delta != null) {
				continue;
			}

			if (cachedEntries == null) {
				cachedEntries = new ArrayList<BatchTiersEntry>();
			}
			final Tiers tiers = value.getValueForParts(params.parts);
			final BatchTiersEntry entry = new BatchTiersEntry(id, tiers);

			cachedEntries.add(entry);
		}

		return cachedEntries;
	}

	/**
	 * {@inheritDoc}
	 */
	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			if (params.tiersNumbers == null || params.tiersNumbers.isEmpty()) {
				return new BatchTiersHisto();
			}

			final BatchTiersHisto batch;

			// on récupère tout ce qu'on peut dans le cache
			final List<BatchTiersHistoEntry> cachedEntries = getCachedBatchTiersHistoEntries(params);
			if (cachedEntries == null || cachedEntries.isEmpty()) {
				// rien trouvé -> on passe tout droit
				batch = target.getBatchTiersHisto(params);

				// on met-à-jour le cache
				cacheBatchTiersHistoEntries(batch, params);
			}
			else {
				// trouvé des données dans le cache -> on détermine ce qui manque
				final Set<Long> uncachedIds = extractUncachedTiersHistoIds(params.tiersNumbers, cachedEntries);
				if (uncachedIds.isEmpty()) {
					batch = new BatchTiersHisto();
				}
				else {
					// on demande plus loin ce qui manque
					batch = target.getBatchTiersHisto(new GetBatchTiersHisto(params.login, uncachedIds, params.parts));

					// on met-à-jour le cache
					cacheBatchTiersHistoEntries(batch, params);
				}

				// on mélange les deux collections
				batch.entries.addAll(cachedEntries);
			}

			return batch;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * Met-à-jour le cache à partir de données nouvellement extraites.
	 *
	 * @param batch  les données nouvellement extraites
	 * @param params les paramètres demandés correspondant aux données spécifiées.
	 */
	private void cacheBatchTiersHistoEntries(BatchTiersHisto batch, GetBatchTiersHisto params) {
		for (BatchTiersHistoEntry entry : batch.entries) {
			if (entry.exceptionMessage != null) {   // [UNIREG-3288] on ignore les tiers qui ont levé une exception
				continue;
			}

			final GetTiersHistoKey key = new GetTiersHistoKey(entry.number);

			final Element element = cache.get(key);
			if (element == null) {
				// on enregistre le nouveau tiers dans le cache
				GetTiersHistoValue value = new GetTiersHistoValue(params.parts, entry.tiers);
				cache.put(new Element(key, value));
			}
			else if (entry.tiers != null) {
				// on met-à-jour le tiers (s'il existe) avec les parts chargées
				GetTiersHistoValue value = (GetTiersHistoValue) element.getObjectValue();
				Assert.isFalse(value.isNull());
				value.addParts(params.parts, entry.tiers);
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
	private Set<Long> extractUncachedTiersHistoIds(Set<Long> requestedIds, List<BatchTiersHistoEntry> cachedEntries) {
		final Set<Long> cachedIds = new HashSet<Long>(cachedEntries.size());
		for (BatchTiersHistoEntry entry : cachedEntries) {
			cachedIds.add(entry.number);
		}
		final Set<Long> uncached = new HashSet<Long>(requestedIds.size() - cachedIds.size());
		for (Long id : requestedIds) {
			if (!cachedIds.contains(id)) {
				uncached.add(id);
			}
		}
		return uncached;
	}

	/**
	 * Retourne les données disponibles dans le cache qui correspondent aux paramètres demandés. Si un tiers existe dans le cache mais que toutes les parts demandées ne sont pas renseignées, on
	 * l'ignore.
	 *
	 * @param params les paramètres de l'appel
	 * @return une liste des données trouvées dans le cache
	 */
	private List<BatchTiersHistoEntry> getCachedBatchTiersHistoEntries(GetBatchTiersHisto params) {

		List<BatchTiersHistoEntry> cachedEntries = null;

		for (Long id : params.tiersNumbers) {
			final GetTiersHistoKey key = new GetTiersHistoKey(id);

			final Element element = cache.get(key);
			if (element == null) {
				continue;
			}

			GetTiersHistoValue value = (GetTiersHistoValue) element.getObjectValue();
			if (value.isNull()) {
				continue;
			}

			Set<TiersPart> delta = value.getMissingParts(params.parts);
			if (delta != null) {
				continue;
			}

			if (cachedEntries == null) {
				cachedEntries = new ArrayList<BatchTiersHistoEntry>();
			}
			final TiersHisto tiers = value.getValueForParts(params.parts);
			final BatchTiersHistoEntry entry = new BatchTiersHistoEntry(id, tiers);

			cachedEntries.add(entry);
		}

		return cachedEntries;
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

	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws BusinessException, AccessDeniedException, TechnicalException {

		final DebiteurInfo resultat;

		final GetDebiteurInfoKey key = new GetDebiteurInfoKey(params.numeroDebiteur, params.periodeFiscale);

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

	@Override
	public List<Long> getListeCtbModifies(GetListeCtbModifies params) throws BusinessException,	AccessDeniedException, TechnicalException {
		//données mouvantes inutile de cacher
		return target.getListeCtbModifies(params);
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

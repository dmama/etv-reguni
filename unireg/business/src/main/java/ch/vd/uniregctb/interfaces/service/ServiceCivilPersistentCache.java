package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.persistentcache.ObjectKey;
import ch.vd.uniregctb.persistentcache.PersistentCache;
import ch.vd.uniregctb.stats.StatsService;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceCivilPersistentCache extends ServiceCivilServiceBase implements UniregCacheInterface, DataEventListener, InitializingBean, DisposableBean, ServiceCivilServiceWrapper {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilPersistentCache.class);

	public static final String CACHE_NAME = "ServiceCivilPersistent";

	private PersistentCache<IndividuCacheValueWithParts> cache;
	private ServiceCivilService target;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private DataEventService dataEventService;

	public void setTarget(ServiceCivilService target) {
		this.target = target;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCache(PersistentCache<IndividuCacheValueWithParts> cache) {
		this.cache = cache;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "service civil persistent";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "CIVIL-PERSISTENT";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.clear();
	}

	private static class GetIndividuKey implements ObjectKey {

		private static final long serialVersionUID = -8068187058991985334L;

		private final long noIndividu;
		private final int annee;

		private GetIndividuKey(long noIndividu, int annee) {
			this.noIndividu = noIndividu;
			this.annee = annee;
		}

		@Override
		public long getId() {
			return noIndividu;
		}

		@Override
		public String getComplement() {
			return String.valueOf(annee);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Individu getIndividu(long noIndividu, int annee, AttributeIndividu... parties) {

		final Individu individu;
		final Set<AttributeIndividu> partiesSet = arrayToSet(parties);

		final GetIndividuKey key = new GetIndividuKey(noIndividu, annee);
		IndividuCacheValueWithParts value = cache.get(key);
		if (value == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			individu = target.getIndividu(noIndividu, annee, parties);
			value = new IndividuCacheValueWithParts(partiesSet, individu);
			cache.put(key, value);
		}
		else {
			// l'élément est en cache, on s'assure qu'on a toutes les parties nécessaires
			Set<AttributeIndividu> delta = value.getMissingParts(partiesSet);
			if (delta != null) {
				// on complète la liste des parts à la volée
				Individu deltaTiers = target.getIndividu(noIndividu, annee, setToArray(delta));
				value.addParts(delta, deltaTiers);
				// on met-à-jour le cache
				cache.put(key, value);
			}
			individu = value.getValueForParts(partiesSet);
		}

		return individu;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, AttributeIndividu... parties) {

		final Set<AttributeIndividu> partiesSet = arrayToSet(parties);

		final Map<Long, Individu> map = new HashMap<Long, Individu>(nosIndividus.size());
		final Set<Long> uncached = new HashSet<Long>(nosIndividus.size());

		// Récupère les individus dans le cache
		for (Long no : nosIndividus) {
			final GetIndividuKey key = new GetIndividuKey(no, annee);
			final IndividuCacheValueWithParts value = cache.get(key);
			if (value == null) {
				// l'élément n'est pas dans le cache -> on doit le demander au service civil
				uncached.add(no);
			}
			else {
				if (value.getMissingParts(partiesSet) == null) {
					// l'élément dans le cache possède toutes les parties demandées -> on le stocke dans le map de résultats
					Individu individu = value.getValueForParts(partiesSet);
					map.put(no, individu);
				}
				else {
					// l'élément dans le cache ne possède *pas* toutes les parties demandées -> on doit le demander au service civil
					uncached.add(no);
				}
			}
		}

		// Effectue l'appel au service pour les individus non-cachés
		if (!uncached.isEmpty()) {
			final List<Individu> list = target.getIndividus(uncached, annee, parties);
			for (Individu ind : list) {
				final long no = ind.getNoTechnique();
				map.put(no, ind);
				// Met-à-jour le cache
				final GetIndividuKey key = new GetIndividuKey(no, annee);
				final IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(partiesSet, ind);
				cache.put(key, value);
			}
		}

		// Retourne les individus ordonnés en utilisant l'ordre des ids
		final List<Individu> individus = new ArrayList<Individu>(nosIndividus.size());
		for (Long no : nosIndividus) {
			Individu ind = map.get(no);
			if (ind != null) {
				individus.add(ind);
			}
		}

		return individus;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isWarmable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onIndividuChange(long numero) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Eviction des données cachées pour l'individu n° " + numero);
		}
		cache.removeAll(numero);
	}

	@Override
	public void onTiersChange(long id) {
		// rien à faire
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		// rien à faire
	}

	@Override
	public void onTruncateDatabase() {
		// rien à faire
	}

	@Override
	public void onLoadDatabase() {
		// rien à faire
	}

	private static AttributeIndividu[] setToArray(Set<AttributeIndividu> delta) {
		if (delta == null) {
			return null;
		}
		final AttributeIndividu[] array = new AttributeIndividu[delta.size()];
		int i = 0;
		for (AttributeIndividu a : delta) {
			array[i++] = a;
		}
		return array;
	}

	private static Set<AttributeIndividu> arrayToSet(AttributeIndividu[] parties) {
		if (parties == null || parties.length == 0) {
			return null;
		}

		return new HashSet<AttributeIndividu>(Arrays.asList(parties));
	}

	@Override
	public ServiceCivilService getTarget() {
		return target;
	}

	@Override
	public ServiceCivilService getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}

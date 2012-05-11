package ch.vd.unireg.interfaces.civil.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.ServiceCivilServiceWrapper;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.CompletePartsCallback;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.stats.StatsService;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceCivilCache implements ServiceCivilRaw, UniregCacheInterface, DataEventListener, InitializingBean, DisposableBean, ServiceCivilServiceWrapper {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private ServiceCivilRaw target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private DataEventService dataEventService;

	public void setTarget(ServiceCivilRaw target) {
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
	public void setDataEventService(DataEventService dataEventService) {
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
		return "service civil";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "CIVIL";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.removeAll();
	}

	private static class GetIndividuKey implements Serializable {

		private static final long serialVersionUID = -1748046591127116308L;

		private final long noIndividu;
		private final RegDate date;

		private GetIndividuKey(long noIndividu, RegDate date) {
			this.noIndividu = noIndividu;
			this.date = date;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetIndividuKey that = (GetIndividuKey) o;

			if (noIndividu != that.noIndividu) return false;
			//noinspection RedundantIfStatement
			if (date != null ? !date.equals(that.date) : that.date != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = (int) (noIndividu ^ (noIndividu >>> 32));
			result = 31 * result + (date != null ? date.hashCode() : 0);
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Individu getIndividu(final long noIndividu, final RegDate date, AttributeIndividu... parties) {

		final Individu individu;
		final Set<AttributeIndividu> partiesSet = arrayToSet(parties);

		final GetIndividuKey key = new GetIndividuKey(noIndividu, date);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			individu = target.getIndividu(noIndividu, date, parties);
			assertPartsPresence(individu, partiesSet);
			final Set<AttributeIndividu> effectiveParts = individu == null ? partiesSet : individu.getAvailableParts(); // le service peut retourner plus de parts que demandé, autant les stocker tout de suite
			IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(effectiveParts, individu);
			cache.put(new Element(key, value));
		}
		else {
			// l'élément est en cache, on s'assure qu'on a toutes les parties nécessaires
			IndividuCacheValueWithParts value = (IndividuCacheValueWithParts) element.getObjectValue();
			individu = value.getValueForPartsAndCompleteIfNeeded(partiesSet, new CompletePartsCallback<Individu, AttributeIndividu>() {
				@Override
				public Individu getDeltaValue(Set<AttributeIndividu> delta) {
					// on complète la liste des parts à la volée
					return target.getIndividu(noIndividu, date, setToArray(delta));
				}

				@Override
				public void postCompletion() {
					// rien à faire
				}
			});
		}

		return individu;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {

		final Set<AttributeIndividu> partiesSet = arrayToSet(parties);

		final Map<Long, Individu> map = new HashMap<Long, Individu>(nosIndividus.size());
		final Set<Long> uncached = new HashSet<Long>(nosIndividus.size());

		// Récupère les individus dans le cache
		for (Long no : nosIndividus) {
			final GetIndividuKey key = new GetIndividuKey(no, date);
			final Element element = cache.get(key);
			if (element == null) {
				// l'élément n'est pas dans le cache -> on doit le demander au service civil
				uncached.add(no);
			}
			else {
				final IndividuCacheValueWithParts value = (IndividuCacheValueWithParts) element.getObjectValue();
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
			final List<Individu> list = target.getIndividus(uncached, date, parties);
			for (Individu ind : list) {
				final long no = ind.getNoTechnique();
				map.put(no, ind);
				// Met-à-jour le cache
				final GetIndividuKey key = new GetIndividuKey(no, date);
				assertPartsPresence(ind, partiesSet);
				final Set<AttributeIndividu> effectiveParts = ind.getAvailableParts(); // le service peut retourner plus de parts que demandé, autant les stocker tout de suite
				final IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(effectiveParts, ind);
				cache.put(new Element(key, value));
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

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		// on ne cache pas ce genre d'info
		return target.getIndividuFromEvent(eventId);
	}

	private static void assertPartsPresence(Individu individu, Set<AttributeIndividu> partiesSet) {
		if (individu != null && partiesSet != null && !individu.getAvailableParts().containsAll(partiesSet)) {
			throw new ProgrammingException("L'individu ne contient pas toutes les parties demandées !");
		}
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
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof GetIndividuKey) {
				GetIndividuKey ki = (GetIndividuKey) k;
				remove = (ki.noIndividu == numero);
			}
			if (remove) {
				cache.remove(k);
			}
		}
	}

	@Override
	public void onPersonneMoraleChange(long id) {
		// rien à faire
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
	public ServiceCivilRaw getTarget() {
		return target;
	}

	@Override
	public ServiceCivilRaw getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}

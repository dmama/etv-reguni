package ch.vd.unireg.interfaces.civil.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.CompletePartsCallback;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.interfaces.civil.IndividuConnector;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.civil.IndividuConnectorWrapper;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.utils.LogLevel;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IndividuConnectorCache implements IndividuConnector, UniregCacheInterface, KeyDumpableCache, CivilDataEventListener, InitializingBean, DisposableBean, IndividuConnectorWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividuConnectorCache.class);

	private IndividuConnector target;
	private Ehcache cache;
	private StatsService statsService;

	public void setTarget(IndividuConnector target) {
		this.target = target;
	}

	public void setCache(Ehcache cache) {
		this.cache = cache;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (cache == null) {
			throw new IllegalArgumentException("Le cache est nul");
		}
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
	}

	@Override
	public String getDescription() {
		return "connecteur des individus";
	}

	@Override
	public void reset() {
		cache.removeAll();
	}

	private static class GetIndividuKey implements Serializable {

		private static final long serialVersionUID = -4126333789362012191L;

		private final long noIndividu;

		private GetIndividuKey(long noIndividu) {
			this.noIndividu = noIndividu;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetIndividuKey that = (GetIndividuKey) o;
			return noIndividu == that.noIndividu;

		}

		@Override
		public int hashCode() {
			return (int) (noIndividu ^ (noIndividu >>> 32));
		}

		@Override
		public String toString() {
			return "GetIndividuKey{" +
					"noIndividu=" + noIndividu +
					'}';
		}
	}

	@Override
	public Individu getIndividu(final long noIndividu, AttributeIndividu... parties) throws IndividuConnectorException {

		final Individu individu;
		final Set<AttributeIndividu> partiesSet = arrayToSet(parties);

		final GetIndividuKey key = new GetIndividuKey(noIndividu);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			individu = target.getIndividu(noIndividu, parties);
			assertPartsPresence(individu, partiesSet);
			final Set<AttributeIndividu> effectiveParts = individu == null ? partiesSet : individu.getAvailableParts(); // le service peut retourner plus de parts que demandé, autant les stocker tout de suite
			IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(effectiveParts, individu);
			cache.put(new Element(key, value));
		}
		else {
			// l'élément est en cache, on s'assure qu'on a toutes les parties nécessaires
			IndividuCacheValueWithParts value = (IndividuCacheValueWithParts) element.getObjectValue();
			individu = value.getValueForPartsAndCompleteIfNeeded(partiesSet, new CompletePartsCallback<Individu, AttributeIndividu>() {
				@NotNull
				@Override
				public Individu getDeltaValue(Set<AttributeIndividu> delta) {
					// on complète la liste des parts à la volée
					final Individu ind = target.getIndividu(noIndividu, setToArray(delta));
					if (ind == null) {
						throw new IndividuConnectorException("Le connecteur des individus ne trouve pas l'individu n°" + noIndividu + " alors que des données le concernant pré-existent dans le cache !");
					}
					return ind;
				}

				@Override
				public void postCompletion() {
					// rien à faire
				}
			});
		}

		return individu;
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws IndividuConnectorException {

		final Set<AttributeIndividu> partiesSet = arrayToSet(parties);

		final Map<Long, Individu> map = new HashMap<>(nosIndividus.size());
		final Set<Long> uncached = new HashSet<>(nosIndividus.size());

		// Récupère les individus dans le cache
		for (Long no : nosIndividus) {
			final GetIndividuKey key = new GetIndividuKey(no);
			final Element element = cache.get(key);
			if (element == null) {
				// l'élément n'est pas dans le cache -> on doit le demander au connecteur des individus
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
					// l'élément dans le cache ne possède *pas* toutes les parties demandées -> on doit le demander au connecteur des individus
					uncached.add(no);
				}
			}
		}

		// Effectue l'appel au service pour les individus non-cachés
		if (!uncached.isEmpty()) {
			final List<Individu> list = target.getIndividus(uncached, parties);
			for (Individu ind : list) {
				final long no = ind.getNoTechnique();
				map.put(no, ind);
				// Met-à-jour le cache
				final GetIndividuKey key = new GetIndividuKey(no);
				assertPartsPresence(ind, partiesSet);
				final Set<AttributeIndividu> effectiveParts = ind.getAvailableParts(); // le service peut retourner plus de parts que demandé, autant les stocker tout de suite
				final IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(effectiveParts, ind);
				cache.put(new Element(key, value));
			}
		}

		// Retourne les individus ordonnés en utilisant l'ordre des ids
		final List<Individu> individus = new ArrayList<>(nosIndividus.size());
		for (Long no : nosIndividus) {
			Individu ind = map.get(no);
			if (ind != null) {
				individus.add(ind);
			}
		}

		return individus;
	}

	@Override
	public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
		// on ne cache pas ce genre d'info
		return target.getIndividuAfterEvent(eventId);
	}

	@Override
	public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws IndividuConnectorException {
		// on ne cache pas ce genre d'info
		return target.getIndividuByEvent(evtId, parties);
	}

	@Override
	public void ping() throws IndividuConnectorException {
		target.ping();
	}

	private static void assertPartsPresence(Individu individu, Set<AttributeIndividu> partiesSet) {
		if (individu != null && partiesSet != null && !individu.getAvailableParts().containsAll(partiesSet)) {
			throw new ProgrammingException("L'individu ne contient pas toutes les parties demandées !");
		}
	}

	@Override
	public boolean isWarmable() {
		return true;
	}

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
	public void onEntrepriseChange(long id) {
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

		return Arrays.stream(parties)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(AttributeIndividu.class)));
	}

	@Override
	public IndividuConnector getTarget() {
		return target;
	}

	@Override
	public IndividuConnector getUltimateTarget() {
		if (target instanceof IndividuConnectorWrapper) {
			return ((IndividuConnectorWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}
}

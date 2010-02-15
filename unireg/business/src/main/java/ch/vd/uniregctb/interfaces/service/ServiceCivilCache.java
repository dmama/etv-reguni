package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.interfaces.model.*;
import ch.vd.uniregctb.stats.StatsService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceCivilCache extends ServiceCivilServiceBase implements UniregCacheInterface, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private ServiceCivilService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	public void setTarget(ServiceCivilService target) {
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

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCached(SERVICE_NAME, cache);
		}
		uniregCacheManager.register(this);
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCached(SERVICE_NAME);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return "service civil";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "CIVIL";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		cache.removeAll();
	}

	private static class GetIndividuKey {

		private long noIndividu;
		private int annee;

		private GetIndividuKey(long noIndividu, int annee) {
			this.noIndividu = noIndividu;
			this.annee = annee;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetIndividuKey that = (GetIndividuKey) o;

			return annee == that.annee && noIndividu == that.noIndividu;
		}

		@Override
		public int hashCode() {
			int result = (int) (noIndividu ^ (noIndividu >>> 32));
			result = 31 * result + annee;
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Individu getIndividu(long noIndividu, int annee, EnumAttributeIndividu... parties) {

		final Individu individu;
		final Set<EnumAttributeIndividu> partiesSet = arrayToSet(parties);

		final GetIndividuKey key = new GetIndividuKey(noIndividu, annee);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			individu = target.getIndividu(noIndividu, annee, parties);
			IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(partiesSet, individu);
			cache.put(new Element(key, value));
		}
		else {
			// l'élément est en cache, on s'assure qu'on a toutes les parties nécessaires
			IndividuCacheValueWithParts value = (IndividuCacheValueWithParts) element.getObjectValue();
			Set<EnumAttributeIndividu> delta = value.getMissingParts(partiesSet);
			if (delta != null) {
				// on complète la liste des parts à la volée
				Individu deltaTiers = target.getIndividu(noIndividu, annee, setToArray(delta));
				value.addParts(delta, deltaTiers);
			}
			individu = value.getValueForParts(partiesSet);
		}

		return individu;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, EnumAttributeIndividu... parties) {
		// cette méthode n'est pas cachée pour l'instant. A faire si la nécessité se fait sentir...
		return target.getIndividus(nosIndividus, date, parties);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Nationalite> getNationalites(long noIndividu, int annee) {

		final Individu individu = getIndividu(noIndividu, annee, EnumAttributeIndividu.NATIONALITE);
		if (individu == null) {
			return null;
		}

		return individu.getNationalites();
	}

	/**
	 * {@inheritDoc}
	 */
	public Origine getOrigine(long noIndividu, int annee) {

		final Individu individu = getIndividu(noIndividu, annee, EnumAttributeIndividu.ORIGINE);
		if (individu == null) {
			return null;
		}

		return individu.getOrigine();
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Permis> getPermis(long noIndividu, int annee) {

		final Individu individu = getIndividu(noIndividu, annee, EnumAttributeIndividu.PERMIS);
		if (individu == null) {
			return null;
		}

		return individu.getPermis();
	}

	/**
	 * {@inheritDoc}
	 */
	public Tutelle getTutelle(long noIndividu, int annee) {

		final Individu individu = getIndividu(noIndividu, annee, EnumAttributeIndividu.TUTELLE);
		if (individu == null) {
			return null;
		}

		return individu.getTutelle();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUp(ServiceCivilService target) {
		throw new NotImplementedException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		throw new NotImplementedException();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isWarmable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void warmCache(List<Individu> individus, RegDate date, EnumAttributeIndividu... parties) {

		final int annee = (date == null ? 2400 : date.year());
		final Set<EnumAttributeIndividu> partiesSet = arrayToSet(parties);
		
		for (Individu i : individus) {
			final long noIndividu = i.getNoTechnique();
			final GetIndividuKey key = new GetIndividuKey(noIndividu, annee);
			IndividuCacheValueWithParts value = new IndividuCacheValueWithParts(partiesSet, i);
			cache.put(new Element(key, value));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onIndividuChange(long numero) {
		super.onIndividuChange(numero);

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

	private static EnumAttributeIndividu[] setToArray(Set<EnumAttributeIndividu> delta) {
		if (delta == null) {
			return null;
		}
		return delta.toArray(new EnumAttributeIndividu[delta.size()]);
	}

	private static Set<EnumAttributeIndividu> arrayToSet(EnumAttributeIndividu[] parties) {
		if (parties == null) {
		return null;
		}

		final Set<EnumAttributeIndividu> set = new HashSet<EnumAttributeIndividu>(parties.length);
		set.addAll(Arrays.asList(parties));
		return set;
	}
}

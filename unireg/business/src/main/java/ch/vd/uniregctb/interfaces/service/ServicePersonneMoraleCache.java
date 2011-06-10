package ch.vd.uniregctb.interfaces.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.stats.StatsService;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServicePersonneMoraleCache extends ServicePersonneMoraleBase implements UniregCacheInterface, InitializingBean, DisposableBean {

	private CacheManager cacheManager;
	private String cacheName;
	private ServicePersonneMoraleService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTarget(ServicePersonneMoraleService target) {
		this.target = target;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheManager(CacheManager manager) {
		this.cacheManager = manager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
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
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initCache();
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
		uniregCacheManager.register(this);
	}

	@Override
	public String getName() {
		return "PM";
	}

	@Override
	public String getDescription() {
		return "service PM";
	}

	@Override
	public void reset() {
		cache.removeAll();
	}

	private static class GetAllIdsKey {

		@Override
		public boolean equals(Object o) {
			return this == o || !(o == null || getClass() != o.getClass());
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> getAllIds() {
		final List<Long> ids;

		final GetAllIdsKey key = new GetAllIdsKey();
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			ids = target.getAllIds();
			cache.put(new Element(key, ids));
		}
		else {
			ids = (List<Long>) element.getObjectValue();
		}

		return ids;
	}

	private static class GetPersonneMoraleByIdKey {
		private final long id;

		private GetPersonneMoraleByIdKey(long id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetPersonneMoraleByIdKey that = (GetPersonneMoraleByIdKey) o;
			return id == that.id;
		}

		@Override
		public int hashCode() {
			return (int) (id ^ (id >>> 32));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {

		final PersonneMorale pm;
		final Set<PartPM> set = arrayToSet(parts);

		final GetPersonneMoraleByIdKey key = new GetPersonneMoraleByIdKey(id);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			pm = target.getPersonneMorale(id, parts);
			PersonneMoraleCacheValueWithParts value = new PersonneMoraleCacheValueWithParts(set, pm);
			cache.put(new Element(key, value));
		}
		else {
			// l'élément est en cache, on s'assure qu'on a toutes les parties nécessaires
			PersonneMoraleCacheValueWithParts value = (PersonneMoraleCacheValueWithParts) element.getObjectValue();
			Set<PartPM> delta = value.getMissingParts(set);
			if (delta != null) {
				// on complète la liste des parts à la volée
				PersonneMorale deltaTiers = target.getPersonneMorale(id, setToArray(set));
				value.addParts(delta, deltaTiers);
			}
			pm = value.getValueForParts(set);
		}

		return pm;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		// pas caché : cela en vaut-il vraiment la peine ?
		return target.getPersonnesMorales(ids, parts);
	}

	private static class GetEtablissementByIdKey {
		private final long id;

		private GetEtablissementByIdKey(long id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetEtablissementByIdKey that = (GetEtablissementByIdKey) o;

			return id == that.id;

		}

		@Override
		public int hashCode() {
			return (int) (id ^ (id >>> 32));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Etablissement getEtablissement(long id) {

		final Etablissement pm;

		final GetEtablissementByIdKey key = new GetEtablissementByIdKey(id);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			pm = target.getEtablissement(id);
			cache.put(new Element(key, pm));
		}
		else {
			pm = (Etablissement) element.getObjectValue();
		}

		return pm;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Etablissement> getEtablissements(List<Long> ids) {
		// pas caché : cela en vaut-il vraiment la peine ?
		return target.getEtablissements(ids);
	}

	@Override
	public List<EvenementPM> findEvenements(long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		// pas caché : cela en vaut-il vraiment la peine ?
		return target.findEvenements(numeroEntreprise, code, minDate, maxDate);
	}

	private static Set<PartPM> arrayToSet(PartPM[] array) {
		if (array == null) {
			return null;
		}

		final Set<PartPM> set = new HashSet<PartPM>(array.length);
		set.addAll(Arrays.asList(array));
		return set;
	}

	private static PartPM[] setToArray(Set<PartPM> set) {
		if (set == null) {
			return null;
		}
		final PartPM[] array = new PartPM[set.size()];
		int i = 0;
		for (PartPM a : set) {
			array[i++] = a;
		}
		return array;
	}
}

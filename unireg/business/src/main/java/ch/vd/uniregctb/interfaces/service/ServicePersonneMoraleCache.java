package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.stats.StatsService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServicePersonneMoraleCache implements ServicePersonneMoraleService, UniregCacheInterface, InitializingBean, DisposableBean {

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

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCached(SERVICE_NAME);
		}
	}

	public void afterPropertiesSet() throws Exception {
		initCache();
		if (statsService != null) {
			statsService.registerCached(SERVICE_NAME, cache);
		}
		uniregCacheManager.register(this);
	}

	public String getName() {
		return "PM";
	}

	public String getDescription() {
		return "service PM";
	}

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

	private static class GetPersonneMoraleByIdAndPartsKey {
		private long id;
		private Set<PartPM> parts;

		private GetPersonneMoraleByIdAndPartsKey(long id, Set<PartPM> parts) {
			this.id = id;
			this.parts = parts;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetPersonneMoraleByIdAndPartsKey that = (GetPersonneMoraleByIdAndPartsKey) o;

			return id == that.id && !(parts != null ? !parts.equals(that.parts) : that.parts != null);
		}

		@Override
		public int hashCode() {
			int result = (int) (id ^ (id >>> 32));
			result = 31 * result + (parts != null ? parts.hashCode() : 0);
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {

		final PersonneMorale pm;

		final Set<PartPM> set = arrayToSet(parts);
		final GetPersonneMoraleByIdAndPartsKey key = new GetPersonneMoraleByIdAndPartsKey(id, set);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			pm = target.getPersonneMorale(id, parts);
			cache.put(new Element(key, pm));
		}
		else {
			pm = (PersonneMorale) element.getObjectValue();
		}

		return pm;
	}

	private static Set<PartPM> arrayToSet(PartPM[] parts) {
		if (parts == null) {
			return null;
		}

		final Set<PartPM> set = new HashSet<PartPM>(parts.length);
		set.addAll(Arrays.asList(parts));
		return set;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		// pas caché : cela en vaut-il vraiment la peine ?
		return target.getPersonnesMorales(ids, parts);
	}

	private static class GetAdresseKey {
		private long id;
		private RegDate date;

		private GetAdresseKey(long id, RegDate date) {
			this.id = id;
			this.date = date;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetAdresseKey that = (GetAdresseKey) o;

			return id == that.id && !(date != null ? !date.equals(that.date) : that.date != null);

		}

		@Override
		public int hashCode() {
			int result = (int) (id ^ (id >>> 32));
			result = 31 * result + (date != null ? date.hashCode() : 0);
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public AdressesPM getAdresses(long noEntreprise, RegDate date) {

		final AdressesPM adresses;

		final GetAdresseKey key = new GetAdresseKey(noEntreprise, date);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			adresses = target.getAdresses(noEntreprise, date);
			cache.put(new Element(key, adresses));
		}
		else {
			adresses = (AdressesPM) element.getObjectValue();
		}

		return adresses;
	}

	private static class GetAdresseHistoKey {
		private long id;

		private GetAdresseHistoKey(long id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetAdresseHistoKey that = (GetAdresseHistoKey) o;

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
	public AdressesPMHisto getAdressesHisto(long noEntreprise) {

		final AdressesPMHisto adresses;

		final GetAdresseHistoKey key = new GetAdresseHistoKey(noEntreprise);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			adresses = target.getAdressesHisto(noEntreprise);
			cache.put(new Element(key, adresses));
		}
		else {
			adresses = (AdressesPMHisto) element.getObjectValue();
		}

		return adresses;
	}

	public List<EvenementPM> findEvenements(Long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		// pas caché : cela en vaut-il vraiment la peine ?
		return target.findEvenements(numeroEntreprise, code, minDate, maxDate);
	}
}

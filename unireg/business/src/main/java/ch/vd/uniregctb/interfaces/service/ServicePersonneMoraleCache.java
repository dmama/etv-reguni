package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.CompletePartsCallback;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.stats.StatsService;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServicePersonneMoraleCache extends ServicePersonneMoraleBase implements UniregCacheInterface, InitializingBean, DisposableBean, DataEventListener {

	private static final Logger LOGGER = Logger.getLogger(ServicePersonneMoraleCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private ServicePersonneMoraleService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private DataEventService dataEventService;

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
		dataEventService.register(this);
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
	public PersonneMorale getPersonneMorale(final Long id, PartPM... parts) {

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
			pm = value.getValueForPartsAndCompleteIfNeeded(set, new CompletePartsCallback<PersonneMorale, PartPM>() {
				@NotNull
				@Override
				public PersonneMorale getDeltaValue(Set<PartPM> delta) {
					// on complète la liste des parts à la volée
					final PersonneMorale pm = target.getPersonneMorale(id, setToArray(set));
					if (pm == null) {
						throw new PersonneMoraleException("Le service PM ne trouve pas la personne morale n°" + id + " alors que des données la concernant pré-existent dans le cache !");
					}
					return pm;
				}

				@Override
				public void postCompletion() {
					// rien à faire
				}
			});
		}

		return pm;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {

		final Set<PartPM> partiesSet = arrayToSet(parts);

		final Map<Long, PersonneMorale> map = new HashMap<Long, PersonneMorale>(ids.size());
		final Set<Long> uncached = new HashSet<Long>(ids.size());

		// Récupère les PMs dans le cache
		for (Long no : ids) {
			final GetPersonneMoraleByIdKey key = new GetPersonneMoraleByIdKey(no);
			final Element element = cache.get(key);
			if (element == null) {
				// l'élément n'est pas dans le cache -> on doit le demander au service PM
				uncached.add(no);
			}
			else {
				final PersonneMoraleCacheValueWithParts value = (PersonneMoraleCacheValueWithParts) element.getObjectValue();
				if (value.getMissingParts(partiesSet) == null) {
					// l'élément dans le cache possède toutes les parties demandées -> on le stocke dans le map de résultats
					PersonneMorale individu = value.getValueForParts(partiesSet);
					map.put(no, individu);
				}
				else {
					// l'élément dans le cache ne possède *pas* toutes les parties demandées -> on doit le demander au service PM
					uncached.add(no);
				}
			}
		}

		// Effectue l'appel au service pour les PMs non-cachées
		if (!uncached.isEmpty()) {
			final List<PersonneMorale> list = target.getPersonnesMorales(new ArrayList<Long>(uncached), parts);
			for (PersonneMorale pm : list) {
				final long no = pm.getNumeroEntreprise();
				map.put(no, pm);
				// Met-à-jour le cache
				final GetPersonneMoraleByIdKey key = new GetPersonneMoraleByIdKey(no);
				final PersonneMoraleCacheValueWithParts value = new PersonneMoraleCacheValueWithParts(partiesSet, pm);
				cache.put(new Element(key, value));
			}
		}

		// Retourne les PMs ordonnés en utilisant l'ordre des ids
		final List<PersonneMorale> pms = new ArrayList<PersonneMorale>(ids.size());
		for (Long no : ids) {
			PersonneMorale pm = map.get(no);
			if (pm != null) {
				pms.add(pm);
			}
		}

		return pms;
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

	@Override
	public void onTiersChange(long id) {
		// rien à faire
	}

	@Override
	public void onIndividuChange(long id) {
		// rien à faire
	}

	@Override
	public void onPersonneMoraleChange(long id) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Eviction des données cachées pour la personne morale n° " + id);
		}
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof GetPersonneMoraleByIdKey) {
				GetPersonneMoraleByIdKey ki = (GetPersonneMoraleByIdKey) k;
				remove = (ki.id == id);
			}
			else if (k instanceof GetEtablissementByIdKey) {
				GetEtablissementByIdKey ki = (GetEtablissementByIdKey) k;
				remove = (ki.id == id);
			}
			else if (k instanceof GetAllIdsKey) {
				// on ne finasse pas 
				remove = true;
			}
			if (remove) {
				cache.remove(k);
			}
		}
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
}

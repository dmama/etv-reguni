package ch.vd.unireg.interfaces.organisation.cache;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.ObjectKey;
import ch.vd.unireg.cache.PersistentCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.interfaces.organisation.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.organisation.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.organisation.ServiceEntrepriseWrapper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivileEvent;
import ch.vd.unireg.stats.StatsService;

public class ServiceEntreprisePersistentCache implements ServiceEntrepriseRaw, UniregCacheInterface, CivilDataEventListener, InitializingBean, DisposableBean, ServiceEntrepriseWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEntreprisePersistentCache.class);

	public static final String CACHE_NAME = "ServiceEntreprisePersistent";

	private PersistentCache<EntrepriseDataCache> cache;
	private PersistentCache<Long> etablissementCache;
	private ServiceEntrepriseRaw target;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private CivilDataEventService dataEventService;

	public void setTarget(ServiceEntrepriseRaw target) {
		this.target = target;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCache(PersistentCache<EntrepriseDataCache> cache) {
		this.cache = cache;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEtablissementCache(PersistentCache<Long> etablissementCache) {
		this.etablissementCache = etablissementCache;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(CivilDataEventService dataEventService) {
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
		dataEventService.unregister(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "service entreprise persistent";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "ENTREPRISE-PERSISTENT";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.clear();
		etablissementCache.clear();
	}

	private static class GetEntrepriseHistoryKey implements ObjectKey {

		private static final long serialVersionUID = -1985928878473056014L;

		private final long noEntreprise;

		private GetEntrepriseHistoryKey(long noEntreprise) {
			this.noEntreprise = noEntreprise;
		}

		@Override
		public long getId() {
			return noEntreprise;
		}

		@Override
		public String getComplement() {
			return "";
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws ServiceEntrepriseException {

		final GetEntrepriseHistoryKey key = new GetEntrepriseHistoryKey(noEntreprise);
		final EntrepriseDataCache value = cache.get(key);
		if (value == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			final EntrepriseCivile entrepriseCivile = target.getEntrepriseHistory(noEntreprise);
			Objects.requireNonNull(entrepriseCivile, String.format("Aucune entreprise civile retournée par le service pour le no: %s", noEntreprise));

			cache.put(key, new EntrepriseDataCache(entrepriseCivile));
			return entrepriseCivile;
		}
		return value.getEntrepriseCivile();
	}

	private static class GetNoEntrepriseFromNoEtablissementKey implements ObjectKey {

		private static final long serialVersionUID = 5394436700031462099L;

		private final long noEtablissement;

		private GetNoEntrepriseFromNoEtablissementKey(long noEtablissement) {
			this.noEtablissement = noEtablissement;
		}

		@Override
		public long getId() {
			return noEtablissement;
		}

		@Override
		public String getComplement() {
			return "";
		}
	}

	@Override
	public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws ServiceEntrepriseException {

		Long noEntreprise;

		final GetNoEntrepriseFromNoEtablissementKey key = new GetNoEntrepriseFromNoEtablissementKey(noEtablissementCivil);
		final Long value = etablissementCache.get(key);
		if (value == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			noEntreprise = target.getNoEntrepriseFromNoEtablissement(noEtablissementCivil);
			Objects.requireNonNull(noEntreprise, String.format("Aucun numéro d'entreprise retourné par le service pour le no d'établissement civil: %s", noEtablissementCivil));
			etablissementCache.put(key, noEntreprise);

			return noEntreprise;
		}
		return value;
	}

	@Override
	public Identifiers getEntrepriseByNoIde(String noide) throws ServiceEntrepriseException {
		// pas caché pour le moment...
		return target.getEntrepriseByNoIde(noide);
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws ServiceEntrepriseException {
		// aucun intérêt à cacher ce genre d'information
		return target.getEntrepriseEvent(noEvenement);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {
		// pas de cache sur les recherches
		return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) {
		return target.validerAnnonceIDE(modele);
	}

	@Override
	public void ping() throws ServiceEntrepriseException {
		target.ping();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onEntrepriseChange(final long numero) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Eviction des données cachées pour l'entreprise n° " + numero);
		}
		cache.removeAll(numero);
		etablissementCache.removeAll(numero);
		etablissementCache.removeValues(new Predicate<Long>() {
			@Override
			public boolean evaluate(Long object) {
				return numero == object;
			}
		});
	}

	@Override
	public void onIndividuChange(long numero) {
		// rien à faire
	}

	@Override
	public ServiceEntrepriseRaw getTarget() {
		return target;
	}

	@Override
	public ServiceEntrepriseRaw getUltimateTarget() {
		if (target instanceof ServiceEntrepriseWrapper) {
			return ((ServiceEntrepriseWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}

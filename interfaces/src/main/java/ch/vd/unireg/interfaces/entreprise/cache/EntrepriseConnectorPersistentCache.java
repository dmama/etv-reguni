package ch.vd.unireg.interfaces.entreprise.cache;

import java.util.Map;
import java.util.Objects;

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
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorException;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorWrapper;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.stats.StatsService;

public class EntrepriseConnectorPersistentCache implements EntrepriseConnector, UniregCacheInterface, CivilDataEventListener, InitializingBean, DisposableBean, EntrepriseConnectorWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseConnectorPersistentCache.class);

	public static final String CACHE_NAME = "EntrepriseConnectorPersistent";

	private PersistentCache<EntrepriseDataCache> cache;
	private PersistentCache<Long> etablissementCache;
	private EntrepriseConnector target;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	public void setTarget(EntrepriseConnector target) {
		this.target = target;
	}

	public void setCache(PersistentCache<EntrepriseDataCache> cache) {
		this.cache = cache;
	}

	public void setEtablissementCache(PersistentCache<Long> etablissementCache) {
		this.etablissementCache = etablissementCache;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
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
	}

	@Override
	public void destroy() throws Exception {
		uniregCacheManager.unregister(this);
		if (statsService != null) {
			statsService.unregisterCache(CACHE_NAME);
		}
	}

	@Override
	public String getDescription() {
		return "connecteur persistent des entreprises";
	}

	@Override
	public String getName() {
		return "ENTREPRISE-PERSISTENT";
	}

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

	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws EntrepriseConnectorException {

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
	public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws EntrepriseConnectorException {

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
	public Identifiers getEntrepriseByNoIde(String noide) throws EntrepriseConnectorException {
		// pas caché pour le moment...
		return target.getEntrepriseByNoIde(noide);
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws EntrepriseConnectorException {
		// aucun intérêt à cacher ce genre d'information
		return target.getEntrepriseEvent(noEvenement);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws EntrepriseConnectorException {
		// pas de cache sur les recherches
		return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) {
		return target.validerAnnonceIDE(modele);
	}

	@Override
	public void ping() throws EntrepriseConnectorException {
		target.ping();
	}

	@Override
	public void onEntrepriseChange(final long numero) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Eviction des données cachées pour l'entreprise n° " + numero);
		}
		cache.removeAll(numero);
		etablissementCache.removeAll(numero);
		etablissementCache.removeValues(object -> numero == object);
	}

	@Override
	public void onIndividuChange(long numero) {
		// rien à faire
	}

	@Override
	public EntrepriseConnector getTarget() {
		return target;
	}

	@Override
	public EntrepriseConnector getUltimateTarget() {
		if (target instanceof EntrepriseConnectorWrapper) {
			return ((EntrepriseConnectorWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}

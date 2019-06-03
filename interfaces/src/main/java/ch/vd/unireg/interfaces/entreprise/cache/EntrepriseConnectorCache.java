package ch.vd.unireg.interfaces.entreprise.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorException;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorWrapper;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.utils.LogLevel;

public class EntrepriseConnectorCache implements EntrepriseConnector, UniregCacheInterface, KeyDumpableCache, CivilDataEventListener, InitializingBean, DisposableBean, EntrepriseConnectorWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseConnectorCache.class);

	private EntrepriseConnector target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private CivilDataEventService civilDataEventService;

	public void setTarget(EntrepriseConnector target) {
		this.target = target;
	}

	public void setCache(Ehcache cache) {
		this.cache = cache;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setCivilDataEventService(CivilDataEventService civilDataEventService) {
		this.civilDataEventService = civilDataEventService;
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
		if (uniregCacheManager != null) {
			uniregCacheManager.register(this);
		}
		civilDataEventService.register(this);
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		if (uniregCacheManager != null) {
			uniregCacheManager.unregister(this);
		}
		civilDataEventService.unregister(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "connecteur des entreprises";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "ENTREPRISE";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.removeAll();
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

	private static class GetEntrepriseHistoryKey implements Serializable {

		private static final long serialVersionUID = 3198014557405952141L;

		private final long noEntreprise;

		private GetEntrepriseHistoryKey(long noEntreprise) {
			this.noEntreprise = noEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetEntrepriseHistoryKey that = (GetEntrepriseHistoryKey) o;
			return noEntreprise == that.noEntreprise;

		}

		@Override
		public int hashCode() {
			return (int) (noEntreprise ^ (noEntreprise >>> 32));
		}

		@Override
		public String toString() {
			return "GetEntrepriseHistoryKey{" +
					"noEntreprise=" + noEntreprise +
					'}';
		}
	}

	private static class GetNoEntrepriseFromNoEtablissementKey implements Serializable {

		private static final long serialVersionUID = 3198014557405952141L;

		private final long noEtablissement;

		private GetNoEntrepriseFromNoEtablissementKey(long noEtablissement) {
			this.noEtablissement = noEtablissement;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final GetNoEntrepriseFromNoEtablissementKey that = (GetNoEntrepriseFromNoEtablissementKey) o;

			return noEtablissement == that.noEtablissement;

		}

		@Override
		public int hashCode() {
			return (int) (noEtablissement ^ (noEtablissement >>> 32));
		}

		@Override
		public String toString() {
			return "GetNoEntrepriseFromNoEtablissementKey{" +
					"noEtablissement=" + noEtablissement +
					'}';
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntrepriseCivile getEntrepriseHistory(final long noEntreprise) throws EntrepriseConnectorException {

		final GetEntrepriseHistoryKey key = new GetEntrepriseHistoryKey(noEntreprise);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			final EntrepriseCivile entrepriseCivile = target.getEntrepriseHistory(noEntreprise);
			Objects.requireNonNull(entrepriseCivile);
			cache.put(new Element(key, entrepriseCivile));
			return entrepriseCivile;
		}
		return (EntrepriseCivile) element.getObjectValue();
	}

	@Override
	public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws EntrepriseConnectorException {
		final GetNoEntrepriseFromNoEtablissementKey key = new GetNoEntrepriseFromNoEtablissementKey(noEtablissementCivil);
		final Element element = cache.get(key);
		if (element == null) {
			// l'élément n'est pas en cache, on le récupère et on l'insère
			final Long noEtablissementRecupere = target.getNoEntrepriseFromNoEtablissement(noEtablissementCivil);
			Objects.requireNonNull(noEtablissementRecupere);
			cache.put(new Element(key, noEtablissementRecupere));
			return noEtablissementRecupere;
		}
		return (Long) element.getObjectValue();
	}

	private static class GetEntrepriseByNoIdeKey implements Serializable {

		private static final long serialVersionUID = 630591634651995670L;

		private final String noide;

		public GetEntrepriseByNoIdeKey(String noide) {
			this.noide = noide;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GetEntrepriseByNoIdeKey)) return false;
			final GetEntrepriseByNoIdeKey that = (GetEntrepriseByNoIdeKey) o;
			return Objects.equals(noide, that.noide);
		}

		@Override
		public int hashCode() {
			return Objects.hash(noide);
		}

		@Override
		public String toString() {
			return "GetEntrepriseByNoIdeKey{" +
					"noide='" + noide + '\'' +
					'}';
		}
	}

	@Override
	public Identifiers getEntrepriseByNoIde(String noide) throws EntrepriseConnectorException {
		final GetEntrepriseByNoIdeKey key = new GetEntrepriseByNoIdeKey(noide);
		final Element element = cache.get(key);
		if (element == null) {
			final Identifiers ids = target.getEntrepriseByNoIde(noide);
			cache.put(new Element(key, ids));
			return ids;
		}
		else {
			return (Identifiers) element.getObjectValue();
		}
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws EntrepriseConnectorException {
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
	public void ping() throws IndividuConnectorException {
		target.ping();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onIndividuChange(long numero) {
		// rien à faire
	}

	@Override
	public void onEntrepriseChange(long id) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Eviction des données cachées pour l'entreprise n° " + id);
		}
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof GetEntrepriseHistoryKey) {
				final GetEntrepriseHistoryKey ko = (GetEntrepriseHistoryKey) k;
				remove = (ko.noEntreprise == id);
			}
			else if (k instanceof GetNoEntrepriseFromNoEtablissementKey) {
				final GetNoEntrepriseFromNoEtablissementKey ks = (GetNoEntrepriseFromNoEtablissementKey) k;
				remove = (ks.noEtablissement == id);
				if (!remove) {
					final Element elt = cache.getQuiet(k);
					final Object value = elt != null ? elt.getObjectValue() : null;
					if (value != null) {
						remove = value.equals(id);
					}
				}
			}
			if (remove) {
				cache.remove(k);
			}
		}
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}
}

package ch.vd.unireg.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.KeyValueDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.data.FiscalDataEventListener;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.utils.LogLevel;

public class SecurityProviderCache implements UniregCacheInterface, KeyDumpableCache, KeyValueDumpableCache, SecurityProviderInterface, FiscalDataEventListener, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityProviderCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private SecurityProviderInterface target;
	private Ehcache cache;
	private DataEventService dataEventService;
	private TiersDAO tiersDAO;
	private DroitAccesDAO droitAccesDAO;
	private UniregCacheManager uniregCacheManager;
	private PlatformTransactionManager transactionManager;

	private boolean preloadTiersIds;

	/**
	 * Ids des tiers existants au démarrage de l'application (ce cache est immutable, son accès ne nécessite donc pas de synchronisation)
	 * <p/>
	 * Si un numéro de tiers n'existe pas dans le cache, c'est que l'information n'est pas disponible et qu'il faut aller la chercher. Si un numéro de tiers existe, il suffit de regarder le booléen
	 * associé pour savoir si le tiers existe ou non.
	 */
	private Set<Long> tiersExistenceCache = Collections.emptySet();

	/**
	 * Ids des tiers créées depuis le démarrage de l'application (l'accès à ce cache doit être synchronisé)
	 */
	private final Map<Long, Boolean> tiersExistenceDeltaCache = new HashMap<>();

	/**
	 * La liste <b>exhaustive</b> des dossiers contrôlés. Si un tiers n'existe pas dans cette liste, c'est qu'il n'est pas protégé.
	 */
	private Set<Long> dossiersControles = Collections.emptySet();

	public void setTarget(SecurityProviderInterface target) {
		this.target = target;
	}

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

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPreloadTiersIds(boolean preloadTiersIds) {
		this.preloadTiersIds = preloadTiersIds;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	private static class KeyGetDroitAcces {
		final String visaOperateur;
		final long tiersId;

		public KeyGetDroitAcces(String visaOperateur, long tiersId) {
			this.visaOperateur = visaOperateur;
			this.tiersId = tiersId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (tiersId ^ (tiersId >>> 32));
			result = prime * result + ((visaOperateur == null) ? 0 : visaOperateur.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetDroitAcces other = (KeyGetDroitAcces) obj;
			if (tiersId != other.tiersId)
				return false;
			if (visaOperateur == null) {
				if (other.visaOperateur != null)
					return false;
			}
			else if (!visaOperateur.equals(other.visaOperateur))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KeyGetDroitAcces{" +
					"visaOperateur='" + visaOperateur + '\'' +
					", tiersId=" + tiersId +
					'}';
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {

		if (!estControle(tiersId)) {
			return Niveau.ECRITURE;
		}

		final Niveau resultat;

		final KeyGetDroitAcces key = new KeyGetDroitAcces(visaOperateur, tiersId);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getDroitAcces(visaOperateur, tiersId);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Niveau) element.getObjectValue();
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Niveau> getDroitsAcces(String visa, List<Long> ids) {

		final List<Niveau> resultat;

		if (!sontControles(ids)) {
			final int size = ids.size();
			resultat = new ArrayList<>(size);
			for (int i = 0; i < size; ++i) {
				if (ids.get(i) == null) {
					resultat.add(null);
				}
				else {
					resultat.add(Niveau.ECRITURE);
				}
			}
		}
		else {
			resultat = target.getDroitsAcces(visa, ids);
		}

		return resultat;
	}

	/**
	 * @param ids les ids de tiers à vérifier.
	 * @return <i>vrai</i> si au moins un des ids spécifié est contrôlé; <i>faux</i> si aucun id n'est contrôlé.
	 */
	private boolean sontControles(List<Long> ids) {
		for (Long id : ids) {
			if (id != null && estControle(id)) {
				return true;
			}
		}
		return false;
	}

	private boolean estControle(long tiersId) {
		final boolean exists = tiersExists(tiersId);
		if (!exists) {
			throw new TiersNotFoundException(tiersId);
		}
		return dossiersControles.contains(tiersId);
	}

	/**
	 * Détermine si un tiers existe ou non dans la base de données Unireg.
	 *
	 * @param id un id de tiers
	 * @return <b>vrai</b> si le tiers existe dans la base de données; <b>faux</b> autrement.
	 */
	private boolean tiersExists(final Long id) {
		Boolean exists = tiersExistenceCache.contains(id); // on essaie tout d'abord dans le cache initial qui ne nécessite pas de synchronisation
		if (!exists) {
			synchronized (tiersExistenceDeltaCache) {
				exists = tiersExistenceDeltaCache.get(id);
			}
		}
		if (exists == null) {
			if (preloadTiersIds) {
				// le tiers n'existe pas dans le cache préloadé -> il n'existe pas non plus dans la base
				exists = Boolean.FALSE;
			}
			else {
				final TransactionTemplate template = new TransactionTemplate(transactionManager);
				template.setReadOnly(true);

				// le tiers n'existe pas dans le cache non-préloadé -> on va chercher cette information dans la base
				exists = template.execute(new TransactionCallback<Boolean>() {
					@Override
					public Boolean doInTransaction(TransactionStatus status) {
						return tiersDAO.exists(id);
					}
				});
				synchronized (tiersExistenceDeltaCache) {
					tiersExistenceDeltaCache.put(id, exists);
				}
			}
		}
		return exists;
	}

	private static class KeyIsGranted {
		final Role role;
		final String visaOperateur;
		final int codeCollectivite;

		public KeyIsGranted(Role role, String visaOperateur, int codeCollectivite) {
			this.role = role;
			this.visaOperateur = visaOperateur;
			this.codeCollectivite = codeCollectivite;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + codeCollectivite;
			result = prime * result + ((role == null) ? 0 : role.hashCode());
			result = prime * result + ((visaOperateur == null) ? 0 : visaOperateur.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyIsGranted other = (KeyIsGranted) obj;
			if (codeCollectivite != other.codeCollectivite)
				return false;
			if (role == null) {
				if (other.role != null)
					return false;
			}
			else if (role != other.role)
				return false;
			if (visaOperateur == null) {
				if (other.visaOperateur != null)
					return false;
			}
			else if (!visaOperateur.equals(other.visaOperateur))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KeyIsGranted{" +
					"role=" + role +
					", visaOperateur='" + visaOperateur + '\'' +
					", codeCollectivite=" + codeCollectivite +
					'}';
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {
		final Boolean resultat;

		final KeyIsGranted key = new KeyIsGranted(role, visaOperateur, codeCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.isGranted(role, visaOperateur, codeCollectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Boolean) element.getObjectValue();
		}

		return resultat;
	}

	@Override
	public void onTiersChange(long id) {
		if (!tiersExistenceCache.contains(id)) {
			synchronized (tiersExistenceDeltaCache) {
				//nouveau tiers sauvé -> on met-à-jour le cache
				tiersExistenceDeltaCache.put(id, Boolean.TRUE);
			}
		}
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		// Supprime tous les éléments cachés sur le tiers spécifié.
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			if (k instanceof KeyGetDroitAcces) {
				KeyGetDroitAcces kk = (KeyGetDroitAcces) k;
				if (kk.tiersId == tiersId) {
					cache.remove(k);
				}
			}
		}

		/*
		 * A ce niveau, on ne sait pas si le droit d'accès à ajouté ou supprimé. Par mesure de précaution, on admet que le tiers spécifié
		 * fait maintenant partie des tiers nouvellement contrôlés et on l'ajoute à la liste. Au pire, on fera un appel en trop au service
		 * pour ce tiers.
		 */
		addToDossiersControles(tiersId);
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		if (type == TypeRapportEntreTiers.APPARTENANCE_MENAGE && dossiersControles.contains(sujetId)) {
			addToDossiersControles(objetId);
		}
	}

	private void addToDossiersControles(long id) {
		if (!dossiersControles.contains(id)) {
			synchronized (this) {   // les trois lignes de code ci-dessous ne doivent pas être executée par plusieurs threads en même temps
				final Set<Long> newSet = new HashSet<>(dossiersControles);
				newSet.add(id);
				dossiersControles = newSet; // l'assignement est atomique en java, pas besoin de locking
			}
		}
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		// rien à faire, car il n'y a pas de droit d'accès particulier sur les immeubles
	}

	@Override
	public void onBatimentChange(long batimentId) {
		// rien à faire, car il n'y a pas de droit d'accès particulier sur les bâtiments
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		// rien à faire, car il n'y a pas de droit d'accès particulier sur les communautés de propriétaires
	}

	@Override
	public void onTruncateDatabase() {
		clearCaches();
	}

	@Override
	public void onLoadDatabase() {
		initCaches();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cache = cacheManager.getCache(cacheName);
		Assert.notNull(cache);
		dataEventService.register(this);
		uniregCacheManager.register(this);
		initCaches();
	}

	@Override
	public void destroy() throws Exception {
		cache = null;
		dataEventService.unregister(this);
		uniregCacheManager.unregister(this);
	}

	/**
	 * Initialise les caches.
	 */
	private void initCaches() {

		// cache.init() -> rien à faire sur le ehcache : il va se construire tout seul à la demande
		tiersExistenceCache = loadTiersIds(); // l'assignement est atomique en java, pas besoin de locking
		dossiersControles = loadDossierControles(); // l'assignement est atomique en java, pas besoin de locking
	}

	@SuppressWarnings({"unchecked"})
	private synchronized Set<Long> loadTiersIds() {

		final Set<Long> newCache;

		if (preloadTiersIds) {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			LOGGER.info("Préchargement du cache des tiers existants...");
			final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
				@Override
				public List<Long> doInTransaction(TransactionStatus status) {
					return tiersDAO.getAllIds();
				}
			});

			newCache = new HashSet<>(ids);
			LOGGER.info("Préchargement du cache des tiers existant terminé.");
		}
		else {
			newCache = Collections.emptySet();
		}

		return newCache;
	}

	@SuppressWarnings({"unchecked"})
	private synchronized Set<Long> loadDossierControles() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		LOGGER.info("Préchargement du cache des dossiers contrôlés...");
		final Set<Long> idsControles = template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				return droitAccesDAO.getContribuablesControles();
			}
		});
		LOGGER.info("Préchargement du cache des dossiers contrôlés terminé.");

		return idsControles;
	}

	/**
	 * Supprime tous les éléments cachés
	 */
	private void clearCaches() {
		cache.removeAll();
		tiersExistenceCache = Collections.emptySet(); // l'assignement est atomique en java, pas besoin de locking
		synchronized (tiersExistenceDeltaCache) {
			tiersExistenceDeltaCache.clear();
		}
		dossiersControles = Collections.emptySet(); // l'assignement est atomique en java, pas besoin de locking
	}

	/**
	 * [UNIREG-2984] Reset les caches en ordonnant bien précautionneusement les opérations pour que le cache sur l'existence des tiers reste aussi cohérent que possible
	 */
	private void resetCaches() {

		cache.removeAll();

		// on recharge le cache principal de l'existence des tiers dans un variable temporaire
		final Set<Long> newIds = loadTiersIds();
		final Set<Long> newDossiers = loadDossierControles();

		// puis on met tout à jour en même temps
		synchronized (tiersExistenceDeltaCache) {
			tiersExistenceCache = newIds;
			tiersExistenceDeltaCache.clear();
		}

		dossiersControles = newDossiers; // l'assignement est atomique en java, pas besoin de locking
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "security provider";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "SECURITY-PROVIDER";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		resetCaches();
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}

	@Override
	public void dumpCacheContent(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeysAndValues(cache, logger, level, null);
	}
}

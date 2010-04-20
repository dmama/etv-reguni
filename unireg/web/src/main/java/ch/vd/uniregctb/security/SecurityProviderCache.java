package ch.vd.uniregctb.security;

import java.util.*;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.database.DatabaseListener;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Niveau;

public class SecurityProviderCache implements UniregCacheInterface, SecurityProviderInterface, DatabaseListener, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(SecurityProviderCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private SecurityProviderInterface target;
	private Ehcache cache;
	private DatabaseService databaseService;
	private TiersDAO tiersDAO;
	private DroitAccesDAO droitAccesDAO;
	private UniregCacheManager uniregCacheManager;
	private PlatformTransactionManager transactionManager;

	private boolean preloadTiersIds;
	
	/**
	 * Caches de l'existence des tiers dans la base.
	 * <p>
	 * Si un numéro de tiers n'existe pas dans le cache, c'est que l'information n'est pas disponible et qu'il faut aller la chercher. Si un
	 * numéro de tiers existe, il suffit de regarder le booléen associé pour savoir si le tiers existe ou non.
	 */
	private Map<Long, Boolean> tiersExistenceCache = Collections.emptyMap(); // ids des tiers existants au démarrage de l'application (ce cache est immutable, son accès ne nécessite donc pas de synchronisation)
	private final Map<Long, Boolean> tiersExistenceDeltaCache = new HashMap<Long, Boolean>(); // ids des tiers créées depuis le démarrage de l'application (l'accès à ce cache doit être synchronisé)

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

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	public void setPreloadTiersIds(boolean preloadTiersIds) {
		this.preloadTiersIds = preloadTiersIds;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	private static class KeyGetDroitAcces {
		String visaOperateur;
		long tiersId;

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
	}

	/**
	 * {@inheritDoc}
	 */
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
	public List<Niveau> getDroitAcces(String visa, List<Long> ids) {

		final List<Niveau> resultat;

		if (!sontControles(ids)) {
			final int size = ids.size();
			resultat = new ArrayList<Niveau>(size);
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
			resultat = target.getDroitAcces(visa, ids);
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
			throw new ObjectNotFoundException("Le tiers id=[" + tiersId + "] n'existe pas");
		}
		return dossiersControles.contains(tiersId);
	}

	/**
	 * @return <b>vrai</b> si le tiers existe dans la base de données; <b>faux</b> autrement.
	 */
	private boolean tiersExists(final Long id) {
		Boolean exists = tiersExistenceCache.get(id); // on essaie tout d'abord dans le cache initial qui ne nécessite pas de synchronisation
		if (exists == null) {
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
				exists = (Boolean) template.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
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
		Role role;
		String visaOperateur;
		int codeCollectivite;

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
			else if (!role.equals(other.role))
				return false;
			if (visaOperateur == null) {
				if (other.visaOperateur != null)
					return false;
			}
			else if (!visaOperateur.equals(other.visaOperateur))
				return false;
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
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

	public void onTiersChange(long id) {
		synchronized (tiersExistenceDeltaCache) {
			//nouveau tiers sauvé -> on met-à-jour le cache
			tiersExistenceDeltaCache.put(id, Boolean.TRUE);
		}
	}

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
		final Set<Long> newSet = new HashSet<Long>(dossiersControles);
		newSet.add(tiersId);
		dossiersControles = newSet; // l'assignement est atomique en java, pas besoin de locking
	}

	public void onTruncateDatabase() {
		clearCaches();
	}

	public void onLoadDatabase() {
		initCaches();
	}

	public void afterPropertiesSet() throws Exception {
		cache = cacheManager.getCache(cacheName);
		Assert.notNull(cache);
		databaseService.register(this);
		uniregCacheManager.register(this);
		initCaches();
	}

	/**
	 * Initialise les caches. Cette méthode doit être appelée protégée des accès concurrents sur les caches par l'appelant.
	 */
	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	private void initCaches() {

		// cache.init() -> rien à faire sur le ehcache : il va se construire tout seul à la demande

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if (preloadTiersIds) {
					LOGGER.info("Préchargement du cache des tiers existants...");
					return tiersDAO.getAllIds();
				}
				return null;
			}
		});

		if (ids != null) {
			Map<Long, Boolean> newCache = new HashMap<Long, Boolean>(ids.size());
			for (Long id : ids) {
				newCache.put(id, Boolean.TRUE);
			}
			tiersExistenceCache = newCache; // l'assignement est atomique en java, pas besoin de locking
		}

		final Set<Long> idsControles = (Set<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				LOGGER.info("Préchargement du cache des dossiers contrôlés...");
				return droitAccesDAO.getContribuablesControles();
			}
		});

		dossiersControles = idsControles; // l'assignement est atomique en java, pas besoin de locking
	}

	/**
	 * Supprime tous les éléments cachés
	 */
	private void clearCaches() {
		cache.removeAll();
		tiersExistenceCache = Collections.emptyMap(); // l'assignement est atomique en java, pas besoin de locking
		synchronized (tiersExistenceDeltaCache) {
			tiersExistenceDeltaCache.clear();
		}
		dossiersControles = Collections.emptySet(); // l'assignement est atomique en java, pas besoin de locking
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return "security provider";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "SECURITY-PROVIDER";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		clearCaches();
		initCaches();
	}
}

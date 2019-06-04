package ch.vd.unireg.tiers.manager;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.KeyValueDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.data.FiscalDataEventListener;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.utils.LogLevel;

public class AutorisationCacheImpl implements AutorisationCache, FiscalDataEventListener, InitializingBean, DisposableBean, UniregCacheInterface, KeyDumpableCache, KeyValueDumpableCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutorisationCacheImpl.class);

	private static class AutorisationKey implements Serializable {

		private static final long serialVersionUID = -760486881677996440L;

		private final String visa;
		private final int oid;
		@Nullable
		private final Long tiersId;

		public AutorisationKey(@Nullable Long tiersId, String visa, int oid) {
			this.visa = visa;
			this.oid = oid;
			this.tiersId = tiersId;
		}

		@Nullable
		public Long getTiersId() {
			return tiersId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final AutorisationKey that = (AutorisationKey) o;

			if (oid != that.oid) return false;
			//noinspection SimplifiableIfStatement
			if (tiersId != null ? !tiersId.equals(that.tiersId) : that.tiersId != null) return false;
			return visa.equals(that.visa);
		}

		@Override
		public int hashCode() {
			int result = visa.hashCode();
			result = 31 * result + oid;
			result = 31 * result + (tiersId != null ? tiersId.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "AutorisationKey{" +
					"visa='" + visa + '\'' +
					", oid=" + oid +
					", tiersId=" + tiersId +
					'}';
		}
	}

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AutorisationManager autorisationManager;
	private PlatformTransactionManager transactionManager;
	private Cache cache;
	private UniregCacheManager uniregCacheManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		uniregCacheManager.register(this);
	}

	@Override
	public void destroy() throws Exception {
		cache = null;
		uniregCacheManager.unregister(this);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	@Override
	@NotNull
	public Autorisations getAutorisations(Long tiersId, String visa, int oid) {
		final Autorisations auth;

		final AutorisationKey key = new AutorisationKey(tiersId, visa, oid);
		final Element element = cache.get(key);
		if (element == null) {
			auth = loadAutorisations(tiersId, visa, oid);
			cache.put(new Element(key, auth));
		}
		else {
			auth = (Autorisations) element.getObjectValue();
		}

		return auth;
	}

	/**
	 * Pour les tests seulement, permet de savoir si le cache contient bien une donnée ou pas
	 */
	protected boolean hasCachedData(Long tiersId, String visa, int oid) {
		final AutorisationKey key = new AutorisationKey(tiersId, visa, oid);
		final Element element = cache.get(key);
		return element != null;
	}

	private Autorisations loadAutorisations(final Long tiersId, final String visa, final int oid) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {
			final Tiers tiers = tiersId == null ? null : tiersDAO.get(tiersId);
			return autorisationManager.getAutorisations(tiers, visa, oid);
		});
	}

	/**
	 * Supprime toutes les données cachées associées au tiers spécifié.
	 *
	 * @param id l'id du tiers dont les données cachées doivent être supprimées.
	 */
	private void evictTiers(long id) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Eviction des autorisations cachées pour le tiers n° " + id);
		}
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			if (k instanceof AutorisationKey) {
				final Long tiersId = ((AutorisationKey) k).getTiersId();
				if (tiersId != null && tiersId.equals(id)) {
					cache.remove(k);
				}
			}
		}
	}

	@Override
	public void onTiersChange(long id) {
		evictTiers(id);

		// [SIFISC-12035] le changement sur un tiers ménage commun peut avoir des conséquences (en termes d'accès) sur les membres du couple
		evictHouseholdMembers(id);

		//[SIFISC-15837] Il faut également mettre à jour les droits sur les tiers liés en cas de presence d'une décision ACI sur le tiers à evicter
		evictTiersLiesParDecisionAci(id);
	}

	/**
	 * Si le tiers possède une décision aci, tous les tiers liés doivent être evictés
	 * @param id
	 */
	private void evictTiersLiesParDecisionAci(final long id) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final Set<Long> otherIds = template.execute(status -> {
			final Tiers tiers = tiersDAO.get(id);
			if (tiers instanceof Contribuable) {
				final Contribuable ctb = (Contribuable) tiers;
				if (ctb.hasDecisionsNonAnnulees()) {
					final Set<Contribuable> listeCtb = tiersService.getContribuablesLies(ctb, null);
					final Set<Long> ids = new HashSet<>(listeCtb.size());
					for (Contribuable c : listeCtb) {
						ids.add(c.getNumero());
					}
					return ids;
				}
			}
			return Collections.emptySet();
		});

		// éviction du cache des tiers liés
		for (long otherId : otherIds) {
			evictTiers(otherId);
		}
	}

	/**
	 * Si le tiers identifié est un ménage commun, force l'éviction du cache de toutes les personnes physiques qui lui sont liées par un rapport
	 * d'appartenance ménage non-annulé
	 * @param id identifiant de tiers
	 */
	private void evictHouseholdMembers(final long id) {

		// récupération des membres qui composent l'éventuel ménage
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final Set<Long> otherIds = template.execute(status -> {
			final Tiers tiers = tiersDAO.get(id);
			if (tiers instanceof MenageCommun) {
				final Set<PersonnePhysique> pps = tiersService.getComposantsMenage((MenageCommun) tiers, null);
				if (pps != null && !pps.isEmpty()) {
					final Set<Long> ids = new HashSet<>(pps.size());
					for (PersonnePhysique pp : pps) {
						ids.add(pp.getNumero());
					}
					return ids;
				}
			}
			return Collections.emptySet();
		});

		// éviction du cache des personnes physiques trouvées
		for (long otherId : otherIds) {
			evictTiers(otherId);
		}
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		// rien à faire (le onTiersChange() est également appelé)
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		evictTiers(tiersId);
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		// rien à faire, il n'y a pas d'autorisation sur les immeubles
	}

	@Override
	public void onBatimentChange(long batimentId) {
		// rien à faire, il n'y a pas d'autorisation sur les bâtiments
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		// rien à faire, il n'y a pas d'autorisation sur les communautés de propriétaires
	}

	@Override
	public void onTruncateDatabase() {
		reset();
	}

	@Override
	public void onLoadDatabase() {
		reset();
	}

	@Override
	public String getName() {
		return "AUTH-WEB";
	}

	@Override
	public String getDescription() {
		return "Cache des autorisations d'édition des tiers";
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	@Override
	public void reset() {
		cache.removeAll();
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

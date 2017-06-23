package ch.vd.uniregctb.webservices.v7.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.avatar.ImageData;
import ch.vd.uniregctb.cache.CacheHelper;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.KeyDumpableCache;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.utils.LogLevel;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;
import ch.vd.uniregctb.webservices.v7.BusinessWebService;
import ch.vd.uniregctb.webservices.v7.PartySearchType;
import ch.vd.uniregctb.webservices.v7.SearchMode;
import ch.vd.uniregctb.xml.ServiceException;

@SuppressWarnings("Duplicates")
public class BusinessWebServiceCache implements BusinessWebService, UniregCacheInterface, KeyDumpableCache, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "WebService7";

	private static final Logger LOGGER = LoggerFactory.getLogger(BusinessWebServiceCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;
	private SecurityProviderInterface securityProvider;
	private BusinessWebService target;

	public final void setTarget(BusinessWebService target) {
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

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	// pour le testing
	protected Ehcache getEhCache() {
		return cache;
	}
	@Override
	public void afterPropertiesSet() throws Exception {
		if (cacheManager == null || cacheName == null) {
			throw new IllegalArgumentException("Properties cacheManager and cacheName should have been set!");
		}
		cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new RuntimeException("Cache could not be initialized!");
		}

		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
		if (uniregCacheManager != null) {
			uniregCacheManager.register(this);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		if (uniregCacheManager != null) {
			uniregCacheManager.unregister(this);
		}
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}

	@Override
	public String getName() {
		return "WS-v7";
	}

	@Override
	public String getDescription() {
		return "web-service v7";
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
	public Party getParty(final UserLogin user, final int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		final GetPartyKey key = new GetPartyKey(partyNo);
		try {
			final Party party;
			final Element element = cache.get(key);
			if (element == null) {
				final Party found = target.getParty(user, partyNo, parts);
				if (found != null) {
					final GetPartyValue value = new GetPartyValue(parts, found);
					cache.put(new Element(key, value));
					party = value.restrictTo(found, parts);
				}
				else {
					party = null;
				}
			}
			else {
				final GetPartyValue value = (GetPartyValue) element.getObjectValue();
				// on complète la liste des parts à la volée
				party = value.getValueForPartsAndCompleteIfNeeded(parts, delta -> target.getParty(user, partyNo, delta));
			}
			return party;
		}
		catch (AccessDeniedException | ServiceException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> nullableParts) throws AccessDeniedException, ServiceException {

		final Parties parties;
		final Set<PartyPart> parts = ensureNotNull(nullableParts);

		// récupération de tout ce qui peut l'être directement dans le cache
		final List<Party> cachedEntries = getCachedParties(partyNos, parts);
		if (cachedEntries == null || cachedEntries.isEmpty()) {
			parties = target.getParties(user, partyNos, parts);
			cacheParties(parties, parts);
		}
		else {
			// quelques données ont été trouvées dans le cache -> voyons maintenant ce qui manque
			final List<Integer> uncachedId = extractUncachedPartyIds(partyNos, cachedEntries);
			if (uncachedId == null || uncachedId.isEmpty()) {
				// rien de plus que ce qui est déjà caché
				parties = new Parties();
			}
			else {
				// on va chercher le surplus par rapport à ce qui est déjà caché
				parties = target.getParties(user, uncachedId, parts);
				cacheParties(parties, parts);
			}

			// ajout des données cachées
			for (Party party : cachedEntries) {
				try {
					WebServiceHelper.checkPartyReadAccess(securityProvider, user, party.getNumber());
					parties.getEntries().add(new Entry(party.getNumber(), party, null));
				}
				catch (AccessDeniedException e) {
					parties.getEntries().add(new Entry(party.getNumber(), null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, e.getMessage())));
				}
			}
		}

		return parties;
	}

	@NotNull
	private static Set<PartyPart> ensureNotNull(@Nullable Set<PartyPart> nullableParts) {
		final Set<PartyPart> parts = EnumSet.noneOf(PartyPart.class);
		if (nullableParts != null) {
			nullableParts.remove(null);
			parts.addAll(nullableParts);
		}
		return parts;
	}

	@Nullable
	private List<Party> getCachedParties(List<Integer> partyNos, Set<PartyPart> parts) {
		final List<Party> cached = new ArrayList<>(partyNos.size());
		for (Integer id : partyNos) {
			final GetPartyKey key = new GetPartyKey(id);

			final Element element = cache.get(key);
			if (element == null) {
				continue;
			}

			final GetPartyValue value = (GetPartyValue) element.getObjectValue();
			if (value.isNull()) {
				continue;
			}

			final Set<PartyPart> delta = value.getMissingParts(parts);
			if (delta != null && !delta.isEmpty()) {
				continue;
			}

			final Party party = value.getValueForParts(parts);
			cached.add(party);
		}
		return cached.isEmpty() ? null : cached;
	}

	private void cacheParties(Parties parties, Set<PartyPart> parts) {
		for (Entry item : parties.getEntries()) {
			// on ne s'intéresse pas aux erreurs...
			final Party party = item.getParty();
			if (party != null) {
				final GetPartyKey key = new GetPartyKey(item.getPartyNo());
				final Element element = cache.get(key);
				if (element == null) {
					// totalement nouveau -> on ajoute
					final GetPartyValue value = new GetPartyValue(parts, party);
					cache.put(new Element(key, value));
				}
				else {
					// données dans le cache à compléter par celle qu'on a maintenant
					final GetPartyValue value = (GetPartyValue) element.getObjectValue();
					Assert.isFalse(value.isNull());
					value.addParts(parts, party);
				}
			}
		}
	}

	private List<Integer> extractUncachedPartyIds(List<Integer> partyNos, List<Party> cached) {
		final List<Integer> cachedIds = new ArrayList<>(cached.size());
		for (Party party : cached) {
			cachedIds.add(party.getNumber());
		}
		final Set<Integer> uncachedIds = new HashSet<>(partyNos);
		uncachedIds.removeAll(cachedIds);
		return new ArrayList<>(uncachedIds);
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException {
		final GetDebtorInfoKey key = new GetDebtorInfoKey(debtorNo, pf);
		final DebtorInfo info;
		final Element element = cache.get(key);
		if (element == null) {
			info = target.getDebtorInfo(user, debtorNo, pf);
			cache.put(new Element(key, info));
		}
		else {
			info = (DebtorInfo) element.getObjectValue();
		}
		return info;
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		return target.getSecurityOnParty(user, partyNo);
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException {
		target.setAutomaticRepaymentBlockingFlag(partyNo, user, blocked);
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException {
		return target.getAutomaticRepaymentBlockingFlag(partyNo, user);
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		return target.ackOrdinaryTaxDeclarations(user, request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, user, request);
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return target.getTaxOffices(municipalityId, date);
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, Date since, Date until) throws AccessDeniedException {
		return target.getModifiedTaxPayers(user, since, until);
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		return target.searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes,
		                          debtorCategory, activeParty, oldWithholdingNumber);
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		return target.getAvatar(partyNo);
	}

	@Override
	public FiscalEvents getFiscalEvents(UserLogin user, int partyNo) throws AccessDeniedException {
		return target.getFiscalEvents(user, partyNo);
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovableProperty(@NotNull UserLogin user, long immoId) throws AccessDeniedException {
		final ImmovableProperty immovable;
		final GetImmovablePropertyKey key = new GetImmovablePropertyKey(immoId);
		final Element element = cache.get(key);
		if (element == null) {
			immovable = target.getImmovableProperty(user, immoId);
			cache.put(new Element(key, immovable));
		}
		else {
			immovable = (ImmovableProperty) element.getObjectValue();
		}
		return immovable;
	}

	@NotNull
	@Override
	public ImmovablePropertyList getImmovableProperties(UserLogin user, List<Long> immoIds) throws AccessDeniedException {
		// FIXME (msi) implémenter ce cache
		return target.getImmovableProperties(user, immoIds);
	}

	@Nullable
	@Override
	public Building getBuilding(@NotNull UserLogin user, long buildingId) throws AccessDeniedException {
		final Building immovable;
		final GetBuildingKey key = new GetBuildingKey(buildingId);
		final Element element = cache.get(key);
		if (element == null) {
			immovable = target.getBuilding(user, buildingId);
			cache.put(new Element(key, immovable));
		}
		else {
			immovable = (Building) element.getObjectValue();
		}
		return immovable;
	}

	@NotNull
	@Override
	public BuildingList getBuildings(@NotNull UserLogin user, List<Long> buildingIds) throws AccessDeniedException {
		// FIXME (msi) implémenter ce cache
		return target.getBuildings(user, buildingIds);
	}

	@Nullable
	@Override
	public CommunityOfOwners getCommunityOfOwners(@NotNull UserLogin user, long communityId) throws AccessDeniedException {
		final CommunityOfOwners community;
		final GetCommunityOfOwnersKey key = new GetCommunityOfOwnersKey(communityId);
		final Element element = cache.get(key);
		if (element == null) {
			community = target.getCommunityOfOwners(user, communityId);
			cache.put(new Element(key, community));
		}
		else {
			community = (CommunityOfOwners) element.getObjectValue();
		}
		return community;
	}

	/**
	 * Vide le cache de toute donnée concernant le tiers dont le numéro est donné
	 *
	 * @param partyNo numéro du tiers à oublier
	 */
	public void evictParty(long partyNo) {
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			if (k instanceof PartyCacheKey && ((PartyCacheKey) k).partyNo == partyNo) {
				cache.remove(k);
			}
		}
	}

	/**
	 * Vide le cache de toutes les données concernant l'immeuble spécifié
	 *
	 * @param immoId l'id technique Unireg de l'immeuble
	 */
	public void evictImmovableProperty(long immoId) {
		final List<?> keys = cache.getKeys();
		keys.stream()
				.filter(k -> k instanceof GetImmovablePropertyKey && ((GetImmovablePropertyKey) k).getImmoId() == immoId)
				.forEach(k -> cache.remove(k));
	}

	/**
	 * Vide le cache de toutes les données concernant le bâtiment spécifié
	 *
	 * @param buildingId l'id technique Unireg du bâtiment
	 */
	public void evictBuilding(long buildingId) {
		final List<?> keys = cache.getKeys();
		keys.stream()
				.filter(k -> k instanceof GetBuildingKey && ((GetBuildingKey) k).getBuildingId() == buildingId)
				.forEach(k -> cache.remove(k));
	}
}

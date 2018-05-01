package ch.vd.unireg.webservices.v7.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.utils.LogLevel;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.common.WebServiceHelper;
import ch.vd.unireg.webservices.v7.BusinessWebService;
import ch.vd.unireg.webservices.v7.PartySearchType;
import ch.vd.unireg.webservices.v7.SearchMode;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.landregistry.v7.BuildingEntry;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersEntry;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyEntry;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertySearchResult;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.error.v1.Error;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

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

	@Nullable
	@Override
	public Party getParty(final int partyNo, @Nullable Set<InternalPartyPart> parts) throws AccessDeniedException, ServiceException {
		final GetPartyKey key = new GetPartyKey(partyNo);
		try {
			final Party party;
			final Element element = cache.get(key);
			if (element == null) {
				final Party found = target.getParty(partyNo, parts);
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
				party = value.getValueForPartsAndCompleteIfNeeded(parts, delta -> target.getParty(partyNo, delta));
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

	@NotNull
	@Override
	public Parties getParties(List<Integer> partyNos, @Nullable Set<InternalPartyPart> nullableParts) throws AccessDeniedException, ServiceException {

		final Parties parties;
		final Set<InternalPartyPart> parts = ensureNotNull(nullableParts);

		// récupération de tout ce qui peut l'être directement dans le cache
		final List<Party> cachedEntries = getCachedParties(partyNos, parts);
		if (cachedEntries == null || cachedEntries.isEmpty()) {
			parties = target.getParties(partyNos, parts);
			cacheParties(parties, parts);
		}
		else {
			// quelques données ont été trouvées dans le cache -> voyons maintenant ce qui manque
			final List<Integer> uncachedId = extractUncachedPartyIds(partyNos, cachedEntries);
			if (uncachedId.isEmpty()) {
				// rien de plus que ce qui est déjà caché
				parties = new Parties();
			}
			else {
				// on va chercher le surplus par rapport à ce qui est déjà caché
				parties = target.getParties(uncachedId, parts);
				cacheParties(parties, parts);
			}

			// ajout des données cachées
			for (Party party : cachedEntries) {
				try {
					WebServiceHelper.checkPartyReadAccess(securityProvider, party.getNumber());
					parties.getEntries().add(new Entry(party.getNumber(), party, null));
				}
				catch (AccessDeniedException e) {
					parties.getEntries().add(new Entry(party.getNumber(), null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, e.getMessage())));
				}
			}
		}

		return parties;
	}

	@Override
	public CommunityOfHeirs getCommunityOfHeirs(int deceasedId) throws AccessDeniedException, ServiceException {
		final CommunityOfHeirs community;
		final GetCommunityOfHeirsKey key = new GetCommunityOfHeirsKey(deceasedId);
		final Element element = cache.get(key);
		if (element == null) {
			community = target.getCommunityOfHeirs(deceasedId);
			cache.put(new Element(key, community));
		}
		else {
			community = (CommunityOfHeirs) element.getObjectValue();
		}
		return community;
	}

	@NotNull
	private static Set<InternalPartyPart> ensureNotNull(@Nullable Set<InternalPartyPart> nullableParts) {
		final Set<InternalPartyPart> parts = EnumSet.noneOf(InternalPartyPart.class);
		if (nullableParts != null) {
			nullableParts.remove(null);
			parts.addAll(nullableParts);
		}
		return parts;
	}

	@Nullable
	private List<Party> getCachedParties(List<Integer> partyNos, Set<InternalPartyPart> parts) {
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

			final Set<InternalPartyPart> delta = value.getMissingParts(parts);
			if (delta != null && !delta.isEmpty()) {
				continue;
			}

			final Party party = value.getValueForParts(parts);
			cached.add(party);
		}
		return cached.isEmpty() ? null : cached;
	}

	private void cacheParties(Parties parties, Set<InternalPartyPart> parts) {
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
					// [SIFISC-28103] on ne doit ajouter que les parts qui manquent
					value.addMissingParts(parts, party);
				}
			}
		}
	}

	@NotNull
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
	public DebtorInfo getDebtorInfo(int debtorNo, int pf) throws AccessDeniedException {
		final GetDebtorInfoKey key = new GetDebtorInfoKey(debtorNo, pf);
		final DebtorInfo info;
		final Element element = cache.get(key);
		if (element == null) {
			info = target.getDebtorInfo(debtorNo, pf);
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
	public SecurityListResponse getSecurityOnParties(@NotNull String user, @NotNull List<Integer> partyNos) {
		return target.getSecurityOnParties(user, partyNos);
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, boolean blocked) throws AccessDeniedException {
		target.setAutomaticRepaymentBlockingFlag(partyNo, blocked);
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo) throws AccessDeniedException {
		return target.getAutomaticRepaymentBlockingFlag(partyNo);
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		return target.ackOrdinaryTaxDeclarations(request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, DeadlineRequest request) throws AccessDeniedException {
		return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, request);
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return target.getTaxOffices(municipalityId, date);
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(Date since, Date until) throws AccessDeniedException {
		return target.getModifiedTaxPayers(since, until);
	}

	@Override
	public List<PartyInfo> searchParty(@Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		return target.searchParty(partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes,
		                          debtorCategory, activeParty, oldWithholdingNumber);
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		return target.getAvatar(partyNo);
	}

	@Override
	public FiscalEvents getFiscalEvents(int partyNo) throws AccessDeniedException {
		return target.getFiscalEvents(partyNo);
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovableProperty(long immoId) throws AccessDeniedException {
		final ImmovableProperty immovable;
		final GetImmovablePropertyKey key = new GetImmovablePropertyKey(immoId);
		final Element element = cache.get(key);
		if (element == null) {
			immovable = target.getImmovableProperty(immoId);
			cache.put(new Element(key, immovable));
		}
		else {
			immovable = (ImmovableProperty) element.getObjectValue();
		}
		return immovable;
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		final ImmovableProperty immovable;
		final GetImmovablePropertyByLocationKey key = new GetImmovablePropertyByLocationKey(municipalityFsoId, parcelNumber, index1, index2, index3);
		final Element element = cache.get(key);
		if (element == null) {
			immovable = target.getImmovablePropertyByLocation(municipalityFsoId, parcelNumber, index1, index2, index3);
			cache.put(new Element(key, immovable));
		}
		else {
			immovable = (ImmovableProperty) element.getObjectValue();
		}
		return immovable;
	}

	@NotNull
	@Override
	public ImmovablePropertySearchResult findImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		// on ne cache pas les recherches
		return target.findImmovablePropertyByLocation(municipalityFsoId, parcelNumber, index1, index2, index3);
	}

	@NotNull
	@Override
	public ImmovablePropertyList getImmovableProperties(List<Long> immoIds) throws AccessDeniedException {

		final List<Long> elementsToFetch = new ArrayList<>();
		final List<ImmovablePropertyEntry> cached = new ArrayList<>();

		// on récupère tout ce qu'on peut dans le cache
		for (Long immoId : immoIds) {
			final Element element = cache.get(new GetImmovablePropertyKey(immoId));
			if (element == null) {
				elementsToFetch.add(immoId);
			}
			else {
				final ImmovableProperty immovableProperty = (ImmovableProperty) element.getObjectValue();
				final Error error = immovableProperty == null ? new Error(ErrorType.BUSINESS, "L'immeuble n°[" + immoId + "] n'existe pas.") : null;
				cached.add(new ImmovablePropertyEntry(immoId, immovableProperty, error));
			}
		}

		// on récupère tout ce qui manque dans le service
		final ImmovablePropertyList list;
		if (elementsToFetch.isEmpty()) {
			list = new ImmovablePropertyList();
		}
		else {
			list = target.getImmovableProperties(elementsToFetch);
		}

		// on met-à-jour le cache
		list.getEntries().forEach(e -> cache.put(new Element(new GetImmovablePropertyKey(e.getImmovablePropertyId()), e.getImmovableProperty())));

		// on fusionne les deux listes
		if (!cached.isEmpty()) {
			list.getEntries().addAll(cached);
			list.getEntries().sort(Comparator.comparing(ImmovablePropertyEntry::getImmovablePropertyId));
		}

		return list;
	}

	@Nullable
	@Override
	public Building getBuilding(long buildingId) throws AccessDeniedException {
		final Building immovable;
		final GetBuildingKey key = new GetBuildingKey(buildingId);
		final Element element = cache.get(key);
		if (element == null) {
			immovable = target.getBuilding(buildingId);
			cache.put(new Element(key, immovable));
		}
		else {
			immovable = (Building) element.getObjectValue();
		}
		return immovable;
	}

	@NotNull
	@Override
	public BuildingList getBuildings(List<Long> buildingIds) throws AccessDeniedException {

		final List<Long> elementsToFetch = new ArrayList<>();
		final List<BuildingEntry> cached = new ArrayList<>();

		// on récupère tout ce qu'on peut dans le cache
		for (Long buildingId : buildingIds) {
			final Element element = cache.get(new GetBuildingKey(buildingId));
			if (element == null) {
				elementsToFetch.add(buildingId);
			}
			else {
				final Building building = (Building) element.getObjectValue();
				final Error error = building == null ? new Error(ErrorType.BUSINESS, "Le bâtiment n°[" + buildingId + "] n'existe pas.") : null;
				cached.add(new BuildingEntry(buildingId, building, error));
			}
		}

		// on récupère tout ce qui manque dans le service
		final BuildingList list;
		if (elementsToFetch.isEmpty()) {
			list = new BuildingList();
		}
		else {
			list = target.getBuildings(elementsToFetch);
		}

		// on met-à-jour le cache
		list.getEntries().forEach(e -> cache.put(new Element(new GetBuildingKey(e.getBuildingId()), e.getBuilding())));

		// on fusionne les deux listes
		if (!cached.isEmpty()) {
			list.getEntries().addAll(cached);
			list.getEntries().sort(Comparator.comparing(BuildingEntry::getBuildingId));
		}

		return list;
	}

	@Nullable
	@Override
	public CommunityOfOwners getCommunityOfOwners(long communityId) throws AccessDeniedException {
		final CommunityOfOwners community;
		final GetCommunityOfOwnersKey key = new GetCommunityOfOwnersKey(communityId);
		final Element element = cache.get(key);
		if (element == null) {
			community = target.getCommunityOfOwners(communityId);
			cache.put(new Element(key, community));
		}
		else {
			community = (CommunityOfOwners) element.getObjectValue();
		}
		return community;
	}

	@NotNull
	@Override
	public CommunityOfOwnersList getCommunitiesOfOwners(List<Long> communityIds) throws AccessDeniedException {

		final List<Long> elementsToFetch = new ArrayList<>();
		final List<CommunityOfOwnersEntry> cached = new ArrayList<>();

		// on récupère tout ce qu'on peut dans le cache
		for (Long communityId : communityIds) {
			final Element element = cache.get(new GetCommunityOfOwnersKey(communityId));
			if (element == null) {
				elementsToFetch.add(communityId);
			}
			else {
				final CommunityOfOwners building = (CommunityOfOwners) element.getObjectValue();
				final Error error = building == null ? new Error(ErrorType.BUSINESS, "La communauté n°[" + communityId + "] n'existe pas.") : null;
				cached.add(new CommunityOfOwnersEntry(communityId, building, error));
			}
		}

		// on récupère tout ce qui manque dans le service
		final CommunityOfOwnersList list;
		if (elementsToFetch.isEmpty()) {
			list = new CommunityOfOwnersList();
		}
		else {
			list = target.getCommunitiesOfOwners(elementsToFetch);
		}

		// on met-à-jour le cache
		list.getEntries().forEach(e -> cache.put(new Element(new GetCommunityOfOwnersKey(e.getCommunityOfOwnersId()), e.getCommunityOfOwners())));

		// on fusionne les deux listes
		if (!cached.isEmpty()) {
			list.getEntries().addAll(cached);
			list.getEntries().sort(Comparator.comparing(CommunityOfOwnersEntry::getCommunityOfOwnersId));
		}

		return list;
	}

	@Override
	public @NotNull String getSwaggerJson() throws IOException {
		final String json;
		final SwaggerJsonKey key = new SwaggerJsonKey();
		final Element element = cache.get(key);
		if (element == null) {
			json = target.getSwaggerJson();
			cache.put(new Element(key, json));
		}
		else {
			json = (String) element.getObjectValue();
		}
		return json;
	}

	/**
	 * Vide le cache des clés de type donné qui satisfont au prédicat donné
	 * @param keyClass classe des clés recherchées
	 * @param keyFilter prédicat sur les clés de ce type
	 * @param <K> type de la clé
	 */
	private <K> void evictFromCache(Class<K> keyClass, Predicate<? super K> keyFilter) {
		final List<?> keys = cache.getKeys();
		keys.stream()
				.filter(keyClass::isInstance)
				.map(keyClass::cast)
				.filter(keyFilter)
				.forEach(cache::remove);
	}

	/**
	 * Vide le cache de toute donnée concernant le tiers dont le numéro est donné
	 *
	 * @param partyNo numéro du tiers à oublier
	 */
	public void evictParty(long partyNo) {
		evictFromCache(PartyCacheKey.class, k -> k.partyNo == partyNo);
	}

	/**
	 * Vide le cache de toutes les données concernant l'immeuble spécifié
	 *
	 * @param immoId l'id technique Unireg de l'immeuble
	 */
	public void evictImmovableProperty(long immoId) {
		evictFromCache(GetImmovablePropertyKey.class, k -> k.getImmoId() == immoId);
		evictFromCache(GetImmovablePropertyByLocationKey.class, k -> true); // on efface tous les immeubles identifiés par leurs situations parce qu'on a pas de critère utilisable
	}

	/**
	 * Vide le cache de toutes les données concernant le bâtiment spécifié
	 *
	 * @param buildingId l'id technique Unireg du bâtiment
	 */
	public void evictBuilding(long buildingId) {
		evictFromCache(GetBuildingKey.class, k -> k.getBuildingId() == buildingId);
	}

	/**
	 * Vide le cache de toutes les données concernant la communauté de propriétaires specifiée
	 */
	public void evictCommunityOfOwners(long communityId) {
		evictFromCache(GetCommunityOfOwnersKey.class, k -> k.getCommunityId() == communityId);
	}
}

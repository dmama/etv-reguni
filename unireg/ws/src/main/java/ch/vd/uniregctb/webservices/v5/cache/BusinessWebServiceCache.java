package ch.vd.uniregctb.webservices.v5.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.modifiedtaxpayers.v1.PartyNumberList;
import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.cache.CacheHelper;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.CompletePartsCallbackWithException;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.KeyDumpableCache;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;
import ch.vd.uniregctb.webservices.v5.BusinessWebService;
import ch.vd.uniregctb.webservices.v5.SearchMode;
import ch.vd.uniregctb.xml.ServiceException;

public class BusinessWebServiceCache implements BusinessWebService, UniregCacheInterface, KeyDumpableCache, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "WebService5";

	private static final Logger LOGGER = Logger.getLogger(BusinessWebServiceCache.class);

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
	public void dumpCacheKeys(Logger logger, Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}

	@Override
	public String getName() {
		return "WS-v5";
	}

	@Override
	public String getDescription() {
		return "web-service v5";
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
				party = value.getValueForPartsAndCompleteIfNeeded(parts, new CompletePartsCallbackWithException<Party, PartyPart>() {
					@Override
					public Party getDeltaValue(Set<PartyPart> delta) throws Exception {
						// on complète la liste des parts à la volée
						return target.getParty(user, partyNo, delta);
					}
				});
			}
			return party;
		}
		catch (AccessDeniedException | ServiceException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
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
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartyType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		return target.searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes, debtorCategory,
		                          activeParty, oldWithholdingNumber);
	}

	@Override
	public List<CorporationEvent> searchCorporationEvent(UserLogin user, @Nullable Integer corporationId, @Nullable String eventCode,
	                                                     @Nullable RegDate startDate, @Nullable RegDate endDate) throws AccessDeniedException, EmptySearchCriteriaException {
		return target.searchCorporationEvent(user, corporationId, eventCode, startDate, endDate);
	}

	/**
	 * Vide le cache de toute donnée concernant le tiers dont le numéro est donné
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
}

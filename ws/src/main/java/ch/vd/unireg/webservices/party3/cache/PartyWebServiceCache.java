package ch.vd.unireg.webservices.party3.cache;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.utils.LogLevel;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.ExtendDeadlineRequest;
import ch.vd.unireg.webservices.party3.ExtendDeadlineResponse;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesResponse;
import ch.vd.unireg.webservices.party3.PartyNumberList;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.webservices.party3.impl.ExceptionHelper;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyType;

public class PartyWebServiceCache implements UniregCacheInterface, KeyDumpableCache, PartyWebService, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "PartyWebService3";

	private static final Logger LOGGER = LoggerFactory.getLogger(PartyWebServiceCache.class);

	private PartyWebService target;
	private Ehcache cache;
	private StatsService statsService;

	public void setTarget(PartyWebService target) {
		this.target = target;
	}

	public void setCache(Ehcache cache) {
		this.cache = cache;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	// pour le testing
	protected Ehcache getEhCache() {
		return cache;
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
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
	}

	@Override
	public Party getParty(final GetPartyRequest params) throws WebServiceException {

		final Party party;

		final GetPartyKey key = new GetPartyKey(params.getPartyNumber());
		final Set<PartyPart> parts = (params.getParts() == null ? null : params.getParts().stream().filter(Objects::nonNull).collect(Collectors.toCollection(() -> EnumSet.noneOf(PartyPart.class))));

		try {
			final Element element = cache.get(key);
			if (element == null) {
				party = target.getParty(params);
				GetPartyValue value = new GetPartyValue(parts, party);
				cache.put(new Element(key, value));
			}
			else {
				GetPartyValue value = (GetPartyValue) element.getObjectValue();
				party = value.getValueForPartsAndCompleteIfNeeded(parts, delta -> {
					// on complète la liste des parts à la volée
					final GetPartyRequest deltaRequest = new GetPartyRequest(params.getLogin(), params.getPartyNumber(), new ArrayList<>(delta));
					return target.getParty(deltaRequest);
				});
			}
		}
		catch (WebServiceException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return party;
	}

	@Override
	public BatchParty getBatchParty(GetBatchPartyRequest params) throws WebServiceException {

		try {
			if (params.getPartyNumbers() == null || params.getPartyNumbers().isEmpty()) {
				return new BatchParty();
			}

			final BatchParty batch;

			// on récupère tout ce qu'on peut dans le cache
			final List<BatchPartyEntry> cachedEntries = getCachedBatchTiersHistoEntries(params);
			if (cachedEntries == null || cachedEntries.isEmpty()) {
				// rien trouvé -> on passe tout droit
				batch = target.getBatchParty(params);

				// on met-à-jour le cache
				cacheBatchPartyEntries(batch, params);
			}
			else {
				// trouvé des données dans le cache -> on détermine ce qui manque
				final List<Integer> uncachedIds = extractUncachedTiersHistoIds(params.getPartyNumbers(), cachedEntries);
				if (uncachedIds.isEmpty()) {
					batch = new BatchParty();
				}
				else {
					// on demande plus loin ce qui manque
					batch = target.getBatchParty(new GetBatchPartyRequest(params.getLogin(), uncachedIds, params.getParts()));

					// on met-à-jour le cache
					cacheBatchPartyEntries(batch, params);
				}

				// on mélange les deux collections
				batch.getEntries().addAll(cachedEntries);
			}

			return batch;
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * Met-à-jour le cache à partir de données nouvellement extraites.
	 *
	 * @param batch  les données nouvellement extraites
	 * @param params les paramètres demandés correspondant aux données spécifiées.
	 */
	private void cacheBatchPartyEntries(BatchParty batch, GetBatchPartyRequest params) {
		final Set<PartyPart> parts = (params.getParts() == null ? null : params.getParts().stream().filter(Objects::nonNull).collect(Collectors.toCollection(() -> EnumSet.noneOf(PartyPart.class))));
		for (BatchPartyEntry entry : batch.getEntries()) {
			if (entry.getExceptionInfo() != null) {   // [UNIREG-3288] on ignore les tiers qui ont levé une exception
				continue;
			}

			final GetPartyKey key = new GetPartyKey(entry.getNumber());

			final Element element = cache.get(key);
			if (element == null) {
				// on enregistre le nouveau tiers dans le cache
				GetPartyValue value = new GetPartyValue(parts, entry.getParty());
				cache.put(new Element(key, value));
			}
			else if (entry.getParty() != null) {
				// on met-à-jour le tiers (s'il existe) avec les parts chargées
				GetPartyValue value = (GetPartyValue) element.getObjectValue();
				if (value.isNull()) {
					throw new IllegalArgumentException();
				}
				// [SIFISC-28103] on ne doit ajouter que les parts qui manquent
				if (parts != null) {
					value.addMissingParts(parts, entry.getParty());
				}
			}
		}
	}

	/**
	 * Extrait l'ensemble des ids de tiers non trouvés dans le cache.
	 *
	 * @param requestedIds  l'ensemble des ids demandés
	 * @param cachedEntries les données trouvées dans le cache
	 * @return l'ensemble des ids non trouvés dans le cache.
	 */
	private List<Integer> extractUncachedTiersHistoIds(List<Integer> requestedIds, List<BatchPartyEntry> cachedEntries) {
		final Set<Integer> cachedIds = new HashSet<>(cachedEntries.size());
		for (BatchPartyEntry entry : cachedEntries) {
			cachedIds.add(entry.getNumber());
		}
		final Set<Integer> uncached = new HashSet<>(requestedIds.size() - cachedIds.size());
		for (Integer id : requestedIds) {
			if (!cachedIds.contains(id)) {
				uncached.add(id);
			}
		}
		return new ArrayList<>(uncached);
	}

	/**
	 * Retourne les données disponibles dans le cache qui correspondent aux paramètres demandés. Si un tiers existe dans le cache mais que toutes les parts demandées ne sont pas renseignées, on
	 * l'ignore.
	 *
	 * @param params les paramètres de l'appel
	 * @return une liste des données trouvées dans le cache
	 */
	private List<BatchPartyEntry> getCachedBatchTiersHistoEntries(GetBatchPartyRequest params) {

		List<BatchPartyEntry> cachedEntries = null;

		final Set<PartyPart> parts = (params.getParts() == null ? null : params.getParts().stream().filter(Objects::nonNull).collect(Collectors.toCollection(() -> EnumSet.noneOf(PartyPart.class))));

		for (Integer id : params.getPartyNumbers()) {
			final GetPartyKey key = new GetPartyKey(id);

			final Element element = cache.get(key);
			if (element == null) {
				continue;
			}

			GetPartyValue value = (GetPartyValue) element.getObjectValue();
			if (value.isNull()) {
				continue;
			}

			Set<PartyPart> delta = value.getMissingParts(parts);
			if (delta != null) {
				continue;
			}

			if (cachedEntries == null) {
				cachedEntries = new ArrayList<>();
			}
			final Party party = value.getValueForParts(parts);
			final BatchPartyEntry entry = new BatchPartyEntry(id, party, null);

			cachedEntries.add(entry);
		}

		return cachedEntries;
	}

	@Override
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {

		final PartyType resultat;

		try {
			final GetPartyTypeKey key = new GetPartyTypeKey(params.getPartyNumber());
			final Element element = cache.get(key);
			if (element == null) {
				resultat = target.getPartyType(params);
				cache.put(new Element(key, resultat));
			}
			else {
				resultat = (PartyType) element.getObjectValue();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return resultat;
	}

	@Override
	public GetTaxOfficesResponse getTaxOffices(GetTaxOfficesRequest params) throws WebServiceException {

		final GetTaxOfficesResponse resultat;

		try {
			final GetTaxOfficesKey key = new GetTaxOfficesKey(params.getMunicipalityFSOId(), params.getDate());
			final Element element = cache.get(key);
			if (element == null) {
				resultat = target.getTaxOffices(params);
				cache.put(new Element(key, resultat));
			}
			else {
				resultat = (GetTaxOfficesResponse) element.getObjectValue();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return resultat;
	}

	@Override
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {
		// pas de cache pour l'instant
		return target.searchParty(params);
	}

	@Override
	public void setAutomaticReimbursementBlocking(SetAutomaticReimbursementBlockingRequest params) throws WebServiceException {
		// pas de valeur retournée -> rien à cacher
		target.setAutomaticReimbursementBlocking(params);
	}

	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {
		// on ne cache pas les événements PM car on ne sait pas quand ils changent
		return target.searchCorporationEvents(params);
	}

	@Override
	public DebtorInfo getDebtorInfo(GetDebtorInfoRequest params) throws WebServiceException {

		final DebtorInfo resultat;

		final GetDebtorInfoKey key = new GetDebtorInfoKey(params.getDebtorNumber(), params.getTaxPeriod());

		try {
			final Element element = cache.get(key);
			if (element == null) {
				resultat = target.getDebtorInfo(params);
				cache.put(new Element(key, resultat));
			}
			else {
				resultat = (DebtorInfo) element.getObjectValue();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}

		return resultat;
	}

	@Override
	public AcknowledgeTaxDeclarationsResponse acknowledgeTaxDeclarations(AcknowledgeTaxDeclarationsRequest params) throws WebServiceException {
		return target.acknowledgeTaxDeclarations(params);
	}

	@Override
	public ExtendDeadlineResponse extendDeadline(ExtendDeadlineRequest request) throws WebServiceException {
		return target.extendDeadline(request);
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public PartyNumberList getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
		//données mouvantes inutile de cacher
		return target.getModifiedTaxpayers(params);
	}

	/**
	 * Supprime tous les éléments cachés sur le tiers spécifié.
	 *
	 * @param id le numéro de tiers
	 */
	public void evictParty(long id) {

		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof CacheKey) {
				CacheKey ck = (CacheKey) k;
				remove = (ck.partyNumber == id);
			}
			if (remove) {
				cache.remove(k);
			}
		}
	}

	/**
	 * Efface complétement le cache.
	 */
	public void clearAll() {
		cache.removeAll();
	}

	@Override
	public String getDescription() {
		return "web-service party v3";
	}

	@Override
	public void reset() {
		cache.removeAll();
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}
}

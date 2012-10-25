package ch.vd.uniregctb.interfaces.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.Status;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Wrapper autour d'un ehcache qui en plus tient des statistiques détaillées par appel.
 */
@SuppressWarnings("unchecked")
public class TracingCache implements Ehcache {

	private static final Logger LOGGER = Logger.getLogger(TracingCache.class);

	private final Ehcache target;

	private long lastDumpStats = 0L;
	private final HashMap<String, Stats> stats = new HashMap<String, Stats>();

	private static class Stats {
		String methodName;
		int nbHits = 0;
		int nbMisses = 0;
	}

	public void dumpStats(Level level) {
		int misses = 0;
		int hits = 0;
		int total = 0;
		if (LOGGER.isEnabledFor(level) || LOGGER.isInfoEnabled()) {
			if (LOGGER.isEnabledFor(level)) {
				LOGGER.log(level, "Statistiques du cache " + getName());
			}
			for (String name : stats.keySet()) {
				Stats stat = stats.get(name);
				if (LOGGER.isEnabledFor(level)) {
					LOGGER.log(level, "   " + name + " : total=" + (stat.nbHits + stat.nbMisses) + " hits=" + stat.nbHits + " misses="
							+ stat.nbMisses);
				}
				misses += stat.nbMisses;
				hits += stat.nbHits;
				total += stat.nbMisses + stat.nbHits;
			}
		}
		if (LOGGER.isInfoEnabled() && total > 0) {
			int percentHits = (hits * 100) / total;
			int percentMisses = 100 - percentHits;
			String message = String.format("Statistiques du cache %s: hits=%3d%% misses=%3d%% total=%d", getName(), percentHits,
					percentMisses, total);
			LOGGER.info(message);
		}
	}

	private void dumpStatsIfNeeded() {
		long now = System.currentTimeMillis();
		if (now - lastDumpStats > 30000) { // Toutes les 30 secondes
			dumpStats(Level.DEBUG);
			lastDumpStats = now;
		}
	}

	private void addMiss(Class<?> c) {
		String name = c.getSimpleName();
		Stats stat = stats.get(name);
		if (stat == null) {
			stat = new Stats();
			stat.methodName = name;
			stats.put(name, stat);
		}
		stat.nbMisses++;

		dumpStatsIfNeeded();
	}

	private void addHit(Class<?> c) {
		String name = c.getSimpleName();
		Stats stat = stats.get(name);
		if (stat == null) {
			stat = new Stats();
			stat.methodName = name;
			stats.put(name, stat);
		}
		stat.nbHits++;
	}

	public TracingCache(Ehcache target) {
		this.target = target;
	}

	@Override
	public void bootstrap() {
		target.bootstrap();
	}

	@Override
	public long calculateInMemorySize() throws IllegalStateException, CacheException {
		return target.calculateInMemorySize();
	}

	@Override
	public void clearStatistics() {
		target.clearStatistics();
	}

	@Override
	public void dispose() throws IllegalStateException {
		target.dispose();
	}

	@Override
	public void evictExpiredElements() {
		target.evictExpiredElements();
	}

	@Override
	public void flush() throws IllegalStateException, CacheException {
		target.flush();
	}

	@Override
	public Element get(Serializable key) throws IllegalStateException, CacheException {
		Element element = target.get(key);
		if (element == null) {
			addMiss(key.getClass());
		}
		else {
			addHit(key.getClass());
		}
		return element;
	}

	@Override
	public Element get(Object key) throws IllegalStateException, CacheException {
		Element element = target.get(key);
		if (element == null) {
			addMiss(key.getClass());
		}
		else {
			addHit(key.getClass());
		}
		return element;
	}

	@Override
	public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
		return target.getAllWithLoader(keys, loaderArgument);
	}

	@Override
	public float getAverageGetTime() {
		return target.getAverageGetTime();
	}

	@Override
	public BootstrapCacheLoader getBootstrapCacheLoader() {
		return target.getBootstrapCacheLoader();
	}

	@Override
	public CacheConfiguration getCacheConfiguration() {
		return target.getCacheConfiguration();
	}

	@Override
	public RegisteredEventListeners getCacheEventNotificationService() {
		return target.getCacheEventNotificationService();
	}

	@Override
	public CacheExceptionHandler getCacheExceptionHandler() {
		return target.getCacheExceptionHandler();
	}

	@Override
	public CacheLoader getCacheLoader() {
		return target.getCacheLoader();
	}

	@Override
	public CacheManager getCacheManager() {
		return target.getCacheManager();
	}

	@Override
	public long getDiskExpiryThreadIntervalSeconds() {
		return target.getDiskExpiryThreadIntervalSeconds();
	}

	@Override
	public int getDiskStoreSize() throws IllegalStateException {
		return target.getDiskStoreSize();
	}

	@Override
	public String getGuid() {
		return target.getGuid();
	}

	@Override
	public List getKeys() throws IllegalStateException, CacheException {
		return target.getKeys();
	}

	@Override
	public List getKeysNoDuplicateCheck() throws IllegalStateException {
		return target.getKeysNoDuplicateCheck();
	}

	@Override
	public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
		return target.getKeysWithExpiryCheck();
	}

	@Override
	public int getMaxElementsInMemory() {
		return target.getMaxElementsInMemory();
	}

	@Override
	public int getMaxElementsOnDisk() {
		return target.getMaxElementsOnDisk();
	}

	@Override
	public MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
		return target.getMemoryStoreEvictionPolicy();
	}

	@Override
	public long getMemoryStoreSize() throws IllegalStateException {
		return target.getMemoryStoreSize();
	}

	@Override
	public String getName() {
		return target.getName();
	}

	@Override
	public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
		return target.getQuiet(key);
	}

	@Override
	public Element getQuiet(Object key) throws IllegalStateException, CacheException {
		return target.getQuiet(key);
	}

	@Override
	public int getSize() throws IllegalStateException, CacheException {
		return target.getSize();
	}

	@Override
	public Statistics getStatistics() throws IllegalStateException {
		return target.getStatistics();
	}

	@Override
	public int getStatisticsAccuracy() {
		return target.getStatisticsAccuracy();
	}

	@Override
	public Status getStatus() {
		return target.getStatus();
	}

	@Override
	public long getTimeToIdleSeconds() {
		return target.getTimeToIdleSeconds();
	}

	@Override
	public long getTimeToLiveSeconds() {
		return target.getTimeToLiveSeconds();
	}

	@Override
	public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
		return target.getWithLoader(key, loader, loaderArgument);
	}

	@Override
	public void initialise() {
		target.initialise();
	}

	@Override
	public boolean isDiskPersistent() {
		return target.isDiskPersistent();
	}

	@Override
	public boolean isElementInMemory(Serializable key) {
		return target.isElementInMemory(key);
	}

	@Override
	public boolean isElementInMemory(Object key) {
		return target.isElementInMemory(key);
	}

	@Override
	public boolean isElementOnDisk(Serializable key) {
		return target.isElementOnDisk(key);
	}

	@Override
	public boolean isElementOnDisk(Object key) {
		return target.isElementOnDisk(key);
	}

	@Override
	public boolean isEternal() {
		return target.isEternal();
	}

	@Override
	public boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
		return target.isExpired(element);
	}

	@Override
	public boolean isKeyInCache(Object key) {
		return target.isKeyInCache(key);
	}

	@Override
	public boolean isOverflowToDisk() {
		return target.isOverflowToDisk();
	}

	@Override
	public boolean isValueInCache(Object value) {
		return target.isValueInCache(value);
	}

	@Override
	public void load(Object key) throws CacheException {
		target.load(key);
	}

	@Override
	public void loadAll(Collection keys, Object argument) throws CacheException {
		target.loadAll(keys, argument);
	}

	@Override
	public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
		target.put(element);
	}

	@Override
	public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
			CacheException {
		target.put(element, doNotNotifyCacheReplicators);
	}

	@Override
	public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
		target.putQuiet(element);
	}

	@Override
	public void registerCacheExtension(CacheExtension cacheExtension) {
		target.registerCacheExtension(cacheExtension);
	}

	@Override
	public boolean remove(Serializable key) throws IllegalStateException {
		return target.remove(key);
	}

	@Override
	public boolean remove(Object key) throws IllegalStateException {
		return target.remove(key);
	}

	@Override
	public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
		return target.remove(key, doNotNotifyCacheReplicators);
	}

	@Override
	public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
		return target.remove(key, doNotNotifyCacheReplicators);
	}

	@Override
	public void removeAll() throws IllegalStateException, CacheException {
		target.removeAll();
	}

	@Override
	public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
		target.removeAll(doNotNotifyCacheReplicators);
	}

	@Override
	public boolean removeQuiet(Serializable key) throws IllegalStateException {
		return target.removeQuiet(key);
	}

	@Override
	public boolean removeQuiet(Object key) throws IllegalStateException {
		return target.removeQuiet(key);
	}

	@Override
	public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
		target.setBootstrapCacheLoader(bootstrapCacheLoader);
	}

	@Override
	public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler) {
		target.setCacheExceptionHandler(cacheExceptionHandler);
	}

	@Override
	public void setCacheLoader(CacheLoader cacheLoader) {
		target.setCacheLoader(cacheLoader);
	}

	@Override
	public void setCacheManager(CacheManager cacheManager) {
		target.setCacheManager(cacheManager);
	}

	@Override
	public void setDiskStorePath(String diskStorePath) throws CacheException {
		target.setDiskStorePath(diskStorePath);
	}

	@Override
	public void setName(String name) {
		target.setName(name);
	}

	@Override
	public void setStatisticsAccuracy(int statisticsAccuracy) {
		target.setStatisticsAccuracy(statisticsAccuracy);
	}

	@Override
	public void unregisterCacheExtension(CacheExtension cacheExtension) {
		target.unregisterCacheExtension(cacheExtension);
	}

	@SuppressWarnings({"CloneDoesntCallSuperClone"})
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new TracingCache((Ehcache) target.clone());
	}
}

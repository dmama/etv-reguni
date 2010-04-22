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

	private class Stats {
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

	public void bootstrap() {
		target.bootstrap();
	}

	public long calculateInMemorySize() throws IllegalStateException, CacheException {
		return target.calculateInMemorySize();
	}

	public void clearStatistics() {
		target.clearStatistics();
	}

	public void dispose() throws IllegalStateException {
		target.dispose();
	}

	public void evictExpiredElements() {
		target.evictExpiredElements();
	}

	public void flush() throws IllegalStateException, CacheException {
		target.flush();
	}

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

	public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
		return target.getAllWithLoader(keys, loaderArgument);
	}

	public float getAverageGetTime() {
		return target.getAverageGetTime();
	}

	public BootstrapCacheLoader getBootstrapCacheLoader() {
		return target.getBootstrapCacheLoader();
	}

	public CacheConfiguration getCacheConfiguration() {
		return target.getCacheConfiguration();
	}

	public RegisteredEventListeners getCacheEventNotificationService() {
		return target.getCacheEventNotificationService();
	}

	public CacheExceptionHandler getCacheExceptionHandler() {
		return target.getCacheExceptionHandler();
	}

	public CacheLoader getCacheLoader() {
		return target.getCacheLoader();
	}

	public CacheManager getCacheManager() {
		return target.getCacheManager();
	}

	public long getDiskExpiryThreadIntervalSeconds() {
		return target.getDiskExpiryThreadIntervalSeconds();
	}

	public int getDiskStoreSize() throws IllegalStateException {
		return target.getDiskStoreSize();
	}

	public String getGuid() {
		return target.getGuid();
	}

	public List getKeys() throws IllegalStateException, CacheException {
		return target.getKeys();
	}

	public List getKeysNoDuplicateCheck() throws IllegalStateException {
		return target.getKeysNoDuplicateCheck();
	}

	public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
		return target.getKeysWithExpiryCheck();
	}

	public int getMaxElementsInMemory() {
		return target.getMaxElementsInMemory();
	}

	public int getMaxElementsOnDisk() {
		return target.getMaxElementsOnDisk();
	}

	public MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
		return target.getMemoryStoreEvictionPolicy();
	}

	public long getMemoryStoreSize() throws IllegalStateException {
		return target.getMemoryStoreSize();
	}

	public String getName() {
		return target.getName();
	}

	public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
		return target.getQuiet(key);
	}

	public Element getQuiet(Object key) throws IllegalStateException, CacheException {
		return target.getQuiet(key);
	}

	public int getSize() throws IllegalStateException, CacheException {
		return target.getSize();
	}

	public Statistics getStatistics() throws IllegalStateException {
		return target.getStatistics();
	}

	public int getStatisticsAccuracy() {
		return target.getStatisticsAccuracy();
	}

	public Status getStatus() {
		return target.getStatus();
	}

	public long getTimeToIdleSeconds() {
		return target.getTimeToIdleSeconds();
	}

	public long getTimeToLiveSeconds() {
		return target.getTimeToLiveSeconds();
	}

	public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
		return target.getWithLoader(key, loader, loaderArgument);
	}

	public void initialise() {
		target.initialise();
	}

	public boolean isDiskPersistent() {
		return target.isDiskPersistent();
	}

	public boolean isElementInMemory(Serializable key) {
		return target.isElementInMemory(key);
	}

	public boolean isElementInMemory(Object key) {
		return target.isElementInMemory(key);
	}

	public boolean isElementOnDisk(Serializable key) {
		return target.isElementOnDisk(key);
	}

	public boolean isElementOnDisk(Object key) {
		return target.isElementOnDisk(key);
	}

	public boolean isEternal() {
		return target.isEternal();
	}

	public boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
		return target.isExpired(element);
	}

	public boolean isKeyInCache(Object key) {
		return target.isKeyInCache(key);
	}

	public boolean isOverflowToDisk() {
		return target.isOverflowToDisk();
	}

	public boolean isValueInCache(Object value) {
		return target.isValueInCache(value);
	}

	public void load(Object key) throws CacheException {
		target.load(key);
	}

	public void loadAll(Collection keys, Object argument) throws CacheException {
		target.loadAll(keys, argument);
	}

	public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
		target.put(element);
	}

	public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
			CacheException {
		target.put(element, doNotNotifyCacheReplicators);
	}

	public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
		target.putQuiet(element);
	}

	public void registerCacheExtension(CacheExtension cacheExtension) {
		target.registerCacheExtension(cacheExtension);
	}

	public boolean remove(Serializable key) throws IllegalStateException {
		return target.remove(key);
	}

	public boolean remove(Object key) throws IllegalStateException {
		return target.remove(key);
	}

	public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
		return target.remove(key, doNotNotifyCacheReplicators);
	}

	public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
		return target.remove(key, doNotNotifyCacheReplicators);
	}

	public void removeAll() throws IllegalStateException, CacheException {
		target.removeAll();
	}

	public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
		target.removeAll(doNotNotifyCacheReplicators);
	}

	public boolean removeQuiet(Serializable key) throws IllegalStateException {
		return target.removeQuiet(key);
	}

	public boolean removeQuiet(Object key) throws IllegalStateException {
		return target.removeQuiet(key);
	}

	public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
		target.setBootstrapCacheLoader(bootstrapCacheLoader);
	}

	public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler) {
		target.setCacheExceptionHandler(cacheExceptionHandler);
	}

	public void setCacheLoader(CacheLoader cacheLoader) {
		target.setCacheLoader(cacheLoader);
	}

	public void setCacheManager(CacheManager cacheManager) {
		target.setCacheManager(cacheManager);
	}

	public void setDiskStorePath(String diskStorePath) throws CacheException {
		target.setDiskStorePath(diskStorePath);
	}

	public void setName(String name) {
		target.setName(name);
	}

	public void setStatisticsAccuracy(int statisticsAccuracy) {
		target.setStatisticsAccuracy(statisticsAccuracy);
	}

	public void unregisterCacheExtension(CacheExtension cacheExtension) {
		target.unregisterCacheExtension(cacheExtension);
	}

	@SuppressWarnings({"CloneDoesntCallSuperClone"})
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new TracingCache((Ehcache) target.clone());
	}
}
